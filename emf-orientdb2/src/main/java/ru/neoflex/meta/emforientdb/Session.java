package ru.neoflex.meta.emforientdb;

import com.orientechnologies.lucene.OLuceneIndexFactory;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.ODirection;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.eclipse.emf.ecore.util.EcoreUtil.isAncestor;

public class Session implements Closeable {
    public static final String EREFERENCE = "EReference";
    public static final String EOBJECT = "EObject";
    public static final String EPROXY = "EProxy";
    public static final String ORIENTDB_SOURCE = "http://orientdb.com/meta";
    public static final String ANN_O_CLASS_NAME = "oClassName";
    private static final Logger logger = LoggerFactory.getLogger(Session.class);
    final Map<Resource, List<OVertex>> savedResourcesMap = new HashMap<>();
    private final SessionFactory factory;
    private final ODatabaseDocument db;
    private final ODatabaseDocumentInternal oldDB;

    Session(SessionFactory factory) {
        this.factory = factory;
        this.oldDB = ODatabaseRecordThreadLocal.instance().getIfDefined();
        this.db = factory.createDatabaseDocument();
    }

    private static void deleteLinks(OVertex delegate) {
        Iterable<OEdge> allEdges = delegate.getEdges(ODirection.BOTH);
        Set<OEdge> items = new HashSet<>();
        for (OEdge edge : allEdges) {
            items.add(edge);
        }
        for (OEdge edge : items) {
            edge.delete();
        }
    }

    @Override
    public void close() {
        db.close();
        if (oldDB != null) {
            ODatabaseRecordThreadLocal.instance().set(oldDB);
        }
    }

    public ODatabaseDocument getDatabaseDocument() {
        return db;
    }

    public SessionFactory getFactory() {
        return factory;
    }

    private OClass getOrCreateEReferenceEdge() {
        OClass oClass = db.getClass(EREFERENCE);
        if (oClass == null) {
            oClass = db.createEdgeClass(EREFERENCE);
            oClass.createProperty("fromFragment", OType.STRING);
            oClass.createProperty("feature", OType.STRING);
            oClass.createProperty("toFragment", OType.STRING);
            oClass.createProperty("index", OType.INTEGER);
            oClass.createProperty("eClass", OType.STRING);
        }
        return oClass;
    }

    public String getOClassName(EClass eClass) {
        String oClassName = getAnnotation(eClass, ANN_O_CLASS_NAME, null);
        if (oClassName != null) {
            return oClassName;
        }
        EPackage ePackage = eClass.getEPackage();
        return ePackage.getNsPrefix() + "_" + eClass.getName();
    }

    private OClass getOrCreateOClass(EClass eClass) {
        String oClassName = getOClassName(eClass);
        OClass oClass = db.getClass(oClassName);
        if (oClass == null) {
            boolean isAbstract = eClass.isAbstract() || isAbstract(eClass);
            oClass = db.createClass(oClassName);
            if (isAbstract) {
                oClass.setAbstract(true);
            }
            if (eClass.getESuperTypes().size() == 0) {
                ensureSuperClass(oClass, getOrCreateEObjectClass());
            }
            for (EClass eSuperClass : eClass.getESuperTypes()) {
                OClass oSuperClass = getOrCreateOClass(eSuperClass);
                ensureSuperClass(oClass, oSuperClass);
            }
        }
        factory.oClassToUriMap.put(oClass.getName(), EcoreUtil.getURI(eClass));
        return oClass;
    }

    private OType convertEDataType(EDataType eDataType) {
        OType oType = OType.getTypeByClass(eDataType.getInstanceClass());
        return oType != null ? oType : OType.STRING;
    }

    private void createProperty(OClass oClass, EStructuralFeature sf) {
        if (sf instanceof EReference) {
            EReference eReference = (EReference) sf;
            if (!eReference.isContainer() && eReference.isContainment()) {
                OClass refOClass = getOrCreateOClass(eReference.getEReferenceType());
                OType oType = eReference.isMany() ? OType.EMBEDDEDLIST : OType.EMBEDDED;
                oClass.createProperty(sf.getName(), oType, refOClass);
            }
        } else {
            EAttribute eAttribute = (EAttribute) sf;
            OType oType = convertEDataType(eAttribute.getEAttributeType());
            if (eAttribute.isMany()) {
                oClass.createProperty(eAttribute.getName(), OType.EMBEDDEDLIST, oType);
            } else {
                oClass.createProperty(eAttribute.getName(), oType);
            }
        }
    }

    public void ensureSuperClass(OClass oClass, OClass oSuperClass) {
        if (!oClass.getAllSuperClasses().contains(oSuperClass)) {
            oClass.addSuperClass(oSuperClass);
        }
    }

    public OClass getOrCreateEObjectClass() {
        OClass oEcoreEObjectClass = db.getClass(EOBJECT);
        if (oEcoreEObjectClass == null) {
            oEcoreEObjectClass = db.createVertexClass(EOBJECT);
            oEcoreEObjectClass.setAbstract(true);
        }
        return oEcoreEObjectClass;
    }

    public OClass getOrCreateEProxyClass() {
        OClass oEProxyClass = db.getClass(EPROXY);
        if (oEProxyClass == null) {
            oEProxyClass = db.createClass(EPROXY);
            ensureSuperClass(oEProxyClass, getOrCreateEObjectClass());
            oEProxyClass.createProperty("uri", OType.STRING);

        }
        return oEProxyClass;
    }

    private String getAnnotation(EModelElement element, String key, String def) {
        EAnnotation eAnnotation = element.getEAnnotation(ORIENTDB_SOURCE);
        if (eAnnotation != null && eAnnotation.getDetails().containsKey(key)) {
            return eAnnotation.getDetails().get(key);
        }
        return def;
    }

    private boolean isAnnotated(EModelElement element, String key, String def, String value) {
        return getAnnotation(element, key, def).equalsIgnoreCase(value);
    }

    private boolean isAbstract(EClass eClass) {
        if (isAnnotated(eClass, "oAbstract", "false", "true")) {
            return true;
        }
        for (EClass eSuperType : eClass.getEAllSuperTypes()) {
            if (isAnnotated(eSuperType, "oAbstract", "false", "true")) {
                return true;
            }
        }
        return false;
    }

    public void createSchema() {
        ((ODatabaseDocumentInternal) db).setUseLightweightEdges(false);
        getOrCreateEProxyClass();
        getOrCreateEReferenceEdge();
        for (EClass eClass : factory.getEClasses()) {
            OClass oClass = getOrCreateOClass(eClass);
//            OProperty idProperty = oClass.getProperty("_id");
//            if (idProperty == null) {
//                oClass.createProperty("_id", OType.STRING);
//            }
            EAttribute id = null;
            for (EStructuralFeature sf : eClass.getEStructuralFeatures()) {
                if (!sf.isDerived() && !sf.isTransient()) {
                    if (sf instanceof EReference && !((EReference) sf).isContainment()) {
                        continue;
                    }
                    OProperty oProperty = oClass.getProperty(sf.getName());
                    if (oProperty == null) {
                        createProperty(oClass, sf);
                    }
                    if (sf instanceof EAttribute) {
                        EAttribute eAttribute = (EAttribute) sf;
                        if (eAttribute.isID()) {
                            id = eAttribute;
                        }
                        createIndexIfRequired(oClass, sf);
                    }
                }
            }
            if (id != null) {
                String name = oClass.getName() + "_" + id.getName() + "_pk";
                if (oClass.getClassIndex(name) == null) {
                    logger.info("Creating unique index " + name);
                    oClass.createIndex(name, OClass.INDEX_TYPE.UNIQUE, id.getName());
                }
            }
            EStructuralFeature qNameFeature = factory.getQNameFeature(eClass);
            if (qNameFeature != null && qNameFeature.getEContainingClass().equals(eClass) && qNameFeature != id) {
                String name = oClass.getName() + "_" + qNameFeature.getName() + "_ak";
                if (oClass.getClassIndex(name) == null) {
                    logger.info("Creating unique index " + name);
                    oClass.createIndex(name, OClass.INDEX_TYPE.UNIQUE, qNameFeature.getName());
                }
            }
        }
    }

    public void createIndexIfRequired(OClass oClass, EStructuralFeature sf) {
        String indexType = getAnnotation(sf, "indexType", null);
        if (indexType != null) {
            String name = oClass.getName() + "_" + sf.getName() + "_ie";
            if (oClass.getClassIndex(name) == null) {
                if (indexType.startsWith("SPATIAL")) {
                    ODocument meta = new ODocument().field("analyzer", StandardAnalyzer.class.getName());
                    oClass.createIndex(name, indexType, null, meta, OLuceneIndexFactory.LUCENE_ALGORITHM, new String[]{sf.getName()});
                } else if (indexType.startsWith("FULLTEXT")) {
                    ODocument meta = new ODocument().field("analyzer", StandardAnalyzer.class.getName());
                    oClass.createIndex(name, indexType, null, meta, OLuceneIndexFactory.LUCENE_ALGORITHM, new String[]{sf.getName()});
                } else {
                    oClass.createIndex(name, indexType, sf.getName());
                }
            }
        }
    }

    private Stream<OElement> loadElements(URI uri) {
        return factory.getORIDs(uri).map(orid -> db.load(orid));
    }

    private Object objectToOObject(EDataType eDataType, Object value) {
        OType oType = convertEDataType(eDataType);
        if (oType == OType.STRING) {
            return EcoreUtil.convertToString(eDataType, value);
        }
        return value;
    }

    private Object oObjectToObject(EDataType eDataType, Object value) {
        OType oType = convertEDataType(eDataType);
        if (oType == OType.STRING) {
            return EcoreUtil.createFromString(eDataType, value.toString());
        }
        if (eDataType.getInstanceClass().isAssignableFrom(Timestamp.class)) {
            return new Timestamp(((Date) value).getTime());
        }
        return value;
    }

    private void populateOElement(EObject eObject, OVertex oElement) {
        populateOElementContainment(eObject, oElement);
        populateOElementCross(eObject, oElement);
    }

    private void populateOElementContainment(EObject eObject, OElement oElement) {
//        oElement.setProperty("_id", EcoreUtil.getURI(eObject).fragment());
        EClass eClass = eObject.eClass();
        for (EStructuralFeature sf : eClass.getEAllStructuralFeatures()) {
            if (!sf.isDerived() && !sf.isTransient()) {
                Object value = eObject.eGet(sf);
                if (sf instanceof EReference && ((EReference) sf).isContainment() && value != null) {
                    List<EObject> eObjects = sf.isMany() ? (List<EObject>) value : Collections.singletonList((EObject) value);
                    List<OElement> embedded = new ArrayList<>();
                    for (EObject cObject : eObjects) {
                        OElement cElement = createOElement(cObject);
                        embedded.add(cElement);
                        populateOElementContainment(cObject, cElement);
                    }
                    oElement.setProperty(sf.getName(), sf.isMany() ? embedded : embedded.get(0), sf.isMany() ? OType.EMBEDDEDLIST : OType.EMBEDDED);
                } else if (sf instanceof EAttribute) {
                    if (eObject.eIsSet(sf)) {
                        if (sf.isMany()) {
                            List eList = (List) value;
                            Stream<Object> oStream = eList.stream().
                                    map(e -> objectToOObject(((EAttribute) sf).getEAttributeType(), e));
                            List<Object> oList = oStream.collect(Collectors.toList());
                            oElement.setProperty(sf.getName(), oList);
                        } else {
                            oElement.setProperty(sf.getName(), objectToOObject(((EAttribute) sf).getEAttributeType(), value));
                        }
                    } else {
                        oElement.removeProperty(sf.getName());
                    }
                } else {
                    oElement.removeProperty(sf.getName());
                }
            } else {
                oElement.removeProperty(sf.getName());
            }
        }
    }

    private void populateOElementCross(EObject eObject, OVertex oElement) {
        Set<OEdge> edges = StreamSupport.stream(oElement.getEdges(ODirection.OUT, EREFERENCE).spliterator(), false).collect(Collectors.toSet());
        new EcoreUtil.CrossReferencer(Collections.singleton(eObject)) {
            {
                crossReference();
            }

            protected void add(InternalEObject internalEObject, EReference eReference, EObject crossReferencedEObject) {
                if (!eReference.isDerived() && !eReference.isTransient() && !eReference.isContainer() && internalEObject.eIsSet(eReference)) {
                    String fromFragment = EcoreUtil.getRelativeURIFragmentPath(eObject, internalEObject);
                    String feature = eReference.getName();
                    EObject crossReferencedRoot = EcoreUtil.getRootContainer(crossReferencedEObject);
                    String toFragment = EcoreUtil.getRelativeURIFragmentPath(crossReferencedRoot, crossReferencedEObject);
                    int index = !eReference.isMany() ? -1 : ((EList) internalEObject.eGet(eReference)).indexOf(crossReferencedEObject);
                    OVertex crVertex = null;
                    if (!isAncestor(emfObjects, crossReferencedEObject)) { // external reference
                        URI crURI = EcoreUtil.getURI(crossReferencedEObject);
                        List<ORID> orids = factory.getORIDs(crURI).collect(Collectors.toList());
                        int oridIndex = crossReferencedRoot.eResource().getContents().indexOf(crossReferencedRoot);
                        crVertex = oridIndex >= 0 && orids.size() > oridIndex && orids.get(oridIndex) != null ?
                                db.load(orids.get(oridIndex)) : createProxyOElement(crURI);
                    } else { // internal reference
                        crVertex = oElement;
                    }
                    String eClassUri = EcoreUtil.getURI(crossReferencedEObject.eClass()).toString();
                    OVertex toVertex = crVertex;
                    Optional<OEdge> oEdgeOpt = edges.stream().filter(e ->
                            e.getProperty("fromFragment").equals(fromFragment)
                            && e.getProperty("feature").equals(feature)
                            && e.getProperty("toFragment").equals(toFragment)
                            && e.getProperty("index").equals(index)
                            && e.getProperty("eClass").equals(eClassUri)
                            && e.getTo().equals(toVertex)
                    ).findFirst();
                    if (oEdgeOpt.isPresent()) {
                        edges.remove(oEdgeOpt.get());
                    }
                    else {
                        OEdge oEdge = oElement.addEdge(crVertex, EREFERENCE);
                        oEdge.setProperty("fromFragment", fromFragment);
                        oEdge.setProperty("feature", feature);
                        oEdge.setProperty("toFragment", toFragment);
                        oEdge.setProperty("index", index);
                        oEdge.setProperty("eClass", eClassUri);
                        oEdge.save();
                    }
                }
            }
        };
        for (OEdge oEdge : edges) {
            if (oEdge.getTo().getSchemaType().get().isSubClassOf(EPROXY)) {
                oEdge.getTo().delete();
            }
            oEdge.delete();
        }
    }

    private OVertex createProxyOElement(URI uri) {
        OVertex oElement = db.newVertex(EPROXY);
        oElement.setProperty("uri", uri);
        oElement.save();
        return oElement;
    }

    private OElement createOElement(EObject eObject) {
        EClass eClass = eObject.eClass();
        String oClassName = getOClassName(eClass);
        return db.newElement(oClassName);
    }

    private OVertex createOVertex(EObject eObject) {
        EClass eClass = eObject.eClass();
        String oClassName = getOClassName(eClass);
        return db.newVertex(oClassName);
    }

    public void delete(URI uri) {
        ResourceSet rs = createResourceSet();
        List<Integer> versions = factory.getVersions(uri).collect(Collectors.toList());
        List<ORID> orids = factory.getORIDs(uri).collect(Collectors.toList());
        for (int i = 0; i < orids.size(); ++i) {
            ORID orid = orids.get(i);
            Integer version = versions.get(i);
            OVertex oVertex = db.load(orid);
            if (oVertex == null) {
                throw new IllegalArgumentException(String.format("Can't delete element with @rid %s", orid.toString()));
            }
            checkVersion(version, oVertex);
            checkDependencies(oVertex);
            Resource resource = rs.createResource(uri);
            EObject eObject = createEObject(rs, oVertex);
            resource.getContents().add(eObject);
            populateEObject(resource.getResourceSet(), oVertex, eObject);
            getFactory().getEvents().fireBeforeDelete(resource);
            // workaround for bug if self-link
            deleteLinks(oVertex);
            oVertex.delete();
        }
        ;
    }

    private void checkDependencies(OVertex oVertex) {
        Set<String> dependent = StreamSupport.stream(oVertex.getEdges(ODirection.IN).spliterator(), false)
                .filter(oEdge -> !oEdge.getFrom().equals(oVertex))
                .map(oEdge -> edgeLabel(oEdge))
                .collect(Collectors.toSet());
        if (dependent.size() > 0) {
            String ids = dependent.stream().collect(Collectors.joining(", "));
            throw new IllegalArgumentException(String.format("Can't delete element %s with references [%s]",
                    elementLabel(oVertex), ids));
        }
    }

    public void save(Resource resource) {
        ResourceSet rs = createResourceSet();
        Resource oldResource = rs.createResource(resource.getURI());
        List<Integer> versions = factory.getVersions(resource.getURI()).collect(Collectors.toList());
        List<ORID> orids = factory.getORIDs(resource.getURI()).collect(Collectors.toList());
        List<OVertex> vertexes = new ArrayList<>();
        for (int i = 0; i < resource.getContents().size(); ++i) {
            EObject eObject = resource.getContents().get(i);
            OVertex oVertex;
            if (i >= orids.size() || orids.get(i) == null) {
                oVertex = createOVertex(eObject);
            } else {
                oVertex = db.load(orids.get(i));
                checkVersion(versions.get(i), oVertex);
                checkDependencies(eObject, oVertex);
                EObject oldObject = createEObject(rs, oVertex);
                oldResource.getContents().add(oldObject);
                populateEObject(rs, oVertex, oldObject);
            }
            vertexes.add(oVertex);
        }
        getFactory().getEvents().fireBeforeSave(oldResource, resource);
        for (int i = 0; i < resource.getContents().size(); ++i) {
            EObject eObject = resource.getContents().get(i);
            OVertex oVertex = vertexes.get(i);
            populateOElement(eObject, oVertex);
            OVertex oRecord = oVertex.save();
            vertexes.set(i, oRecord);
        }
        getFactory().getEvents().fireAfterSave(oldResource, resource);
        resource.setURI(factory.createResourceURI(vertexes));
        savedResourcesMap.put(resource, vertexes);
    }

    private static String elementLabel(OElement oElement) {
        return oElement.getSchemaType().get().getName() + "(" + oElement.getIdentity().toString() + ")";
    }

    private String edgeLabel(OEdge oEdge) {
        EClass eClass = (EClass) createResourceSet().getEObject(URI.createURI(oEdge.getProperty("eClass")), false);
        return elementLabel(oEdge.getFrom()) + oEdge.getProperty("fromFragment") +
                "." + oEdge.getProperty("feature") + "->" + getOClassName(eClass) + "(" +
                oEdge.getProperty("toFragment") + ")";
    }

    private void checkDependencies(EObject eObject, OVertex oVertex) {
        Set<String> dependent = StreamSupport.stream(oVertex.getEdges(ODirection.IN).spliterator(), false)
                .filter(oEdge -> !oEdge.getFrom().equals(oVertex))
                .filter(oEdge -> {
                    String toFragment = oEdge.getProperty("toFragment");
                    URI classURI = URI.createURI(oEdge.getProperty("eClass"));
                    EObject target = StringUtils.isEmpty(toFragment) ? eObject : EcoreUtil.getEObject(eObject, toFragment);
                    return target == null || target.eClass() !=
                            eObject.eResource().getResourceSet().getEObject(classURI, false);
                })
                .map(oEdge -> edgeLabel(oEdge))
                .collect(Collectors.toSet());
        if (dependent.size() > 0) {
            String ids = dependent.stream().collect(Collectors.joining(", "));
            throw new IllegalArgumentException(String.format("Can't save element %s with broken references [%s]",
                    elementLabel(oVertex), ids));
        }
    }

    public void checkVersion(Integer version, OVertex oElement) {
        if (oElement.getVersion() != version) {
            throw new ConcurrentModificationException("OElement has been modified.\n" +
                    "Database version is " + oElement.getVersion() + ", record version is " +
                    version);
        }
    }

    public void load(Resource resource) {
        resource.getContents().clear();
        List<OElement> elements = factory.getORIDs(resource.getURI())
                .map(orid -> (OElement) db.load(orid)).collect(Collectors.toList());
        elements.forEach(oElement -> {
            EObject eObject = createEObject(resource.getResourceSet(), oElement);
            resource.getContents().add(eObject);
            populateEObject(resource.getResourceSet(), (OVertex) oElement, eObject);
        });
        resource.setURI(factory.createResourceURI(elements));
        getFactory().getEvents().fireAfterLoad(resource);
    }

    public EObject createEObject(ResourceSet rs, OElement oElement) {
        OClass oClass = oElement.getSchemaType().get();
        URI eClassURI = factory.oClassToUriMap.get(oClass.getName());
        if (eClassURI == null) {
            throw new IllegalArgumentException("Can't find URI for class " + oClass.getName());
        }
        EClass eClass = (EClass) rs.getEObject(eClassURI, false);
        EObject eObject = EcoreUtil.create(eClass);
        return eObject;
    }

    private void populateEObject(ResourceSet rs, OVertex oElement, EObject eObject) {
        populateEObjectContains(rs, oElement, eObject);
        populateEObjectRefers(rs, oElement, eObject);
    }

    private void populateEObjectRefers(ResourceSet rs, OVertex oElement, EObject eObject) {
        List<OEdge> oEdges = new ArrayList<>();
        oElement.getEdges(ODirection.OUT, EREFERENCE).forEach(oEdge -> oEdges.add(oEdge));
        Collections.sort(oEdges, Comparator.comparing(oEdge -> oEdge.getProperty("index")));
        for (OEdge oEdge : oEdges) {
            String fromFragment = oEdge.getProperty("fromFragment");
            String feature = oEdge.getProperty("feature");
            EObject internalEObject = StringUtils.isEmpty(fromFragment) ? eObject : EcoreUtil.getEObject(eObject, fromFragment);
            if (internalEObject == null) {
                throw new IllegalArgumentException("Owner object not found for feature " + feature);
            }
            EReference sf = (EReference) internalEObject.eClass().getEStructuralFeature(feature);
            if (sf == null) {
                continue;
            }
            OVertex oEdgeTo = oEdge.getTo();
            String eClassURI = oEdge.getProperty("eClass");
            EClass eClass = (EClass) rs.getEObject(URI.createURI(eClassURI), false);
            String toFragment = oEdge.getProperty("toFragment");
            EObject crossReferencedEObject = null;
            if (oEdgeTo.equals(oElement)) {
                crossReferencedEObject = StringUtils.isEmpty(toFragment) ?
                        eObject : EcoreUtil.getEObject(eObject, toFragment);
            } else {
                crossReferencedEObject = EcoreUtil.create(eClass);
                URI crURI = null;
                if (oEdgeTo.getSchemaType().get().isSubClassOf(EPROXY)) {
                    String uri = oEdgeTo.getProperty("uri");
                    crURI = URI.createURI(uri);
                } else {
                    crURI = factory.createResourceURI(oEdgeTo).appendFragment(
                            StringUtils.isNotEmpty(toFragment) ? "//" + toFragment : "/");
                }
                ((InternalEObject) crossReferencedEObject).eSetProxyURI(crURI);
            }
            if (sf.isMany()) {
                ((EList) internalEObject.eGet(sf)).add(crossReferencedEObject);
            } else {
                internalEObject.eSet(sf, crossReferencedEObject);
            }
        }
    }

    private OElement queryElement(String sql, Object... args) {
        OElement oElement = null;
        try (OResultSet oResultSet = db.query(sql, args)) {
            while (oResultSet.hasNext()) {
                OResult oResult = oResultSet.next();
                Optional<OElement> oElementOpt = oResult.getElement();
                if (oElementOpt.isPresent()) {
                    oElement = oElementOpt.get();
                    break;
                }
            }

        }
        return oElement;
    }

    private void populateEObjectContains(ResourceSet rs, OElement oElement, EObject eObject) {
//        ((OrientDBResource) eObject.eResource()).setID(eObject, oElement.getProperty("_id"));
        EClass eClass = eObject.eClass();
        Set<String> propertyNames = oElement.getPropertyNames();
        for (EStructuralFeature sf : eClass.getEAllStructuralFeatures()) {
            if (!sf.isDerived() && !sf.isTransient()) {
                if (!propertyNames.contains(sf.getName())) {
                    if (!(sf instanceof EReference) || !((EReference) sf).isContainer()) {
                        eObject.eUnset(sf);
                    }
                    continue;
                }
                Object value = oElement.getProperty(sf.getName());
                if (sf instanceof EAttribute) {
                    EDataType eDataType = ((EAttribute) sf).getEAttributeType();
                    if (sf.isMany()) {
                        List oObjects = (List) value;
                        Stream<Object> objectStream = oObjects.stream().map(o -> oObjectToObject(eDataType, o));
                        List eObjects = objectStream.collect(Collectors.toList());
                        eObject.eSet(sf, eObjects);
                    } else {
                        eObject.eSet(sf, oObjectToObject(eDataType, value));
                    }
                } else if (sf instanceof EReference) {
                    EReference eReference = (EReference) sf;
                    if (eReference.isContainment()) {
                        if (sf.isMany()) {
                            for (OElement crVertex : (List<OElement>) value) {
                                setContainmentReference(rs, eObject, eReference, crVertex);
                            }
                        } else {
                            setContainmentReference(rs, eObject, eReference, (OElement) value);
                        }
                    }
                }
            }
        }
    }

    private void setContainmentReference(ResourceSet rs, EObject eObject, EReference sf, OElement crVertex) {
        EObject crObject = createEObject(rs, crVertex);
        if (sf.isMany()) {
            ((EList) eObject.eGet(sf)).add(crObject);
        } else {
            eObject.eSet(sf, crObject);
        }
        if (crObject.eIsProxy()) {
            if (!sf.isResolveProxies()) {
                EcoreUtil.resolve(crObject, rs);
            }
        } else {
            populateEObjectContains(rs, crVertex, crObject);
        }
    }

    public ResourceSet createResourceSet() {
        ResourceSet resourceSet = factory.createResourceSet();
        resourceSet.getURIConverter()
                .getURIHandlers()
                .add(0, new OrientDBHandler(this));
        return resourceSet;
    }

    private void getResourceList(OResultSet oResultSet, Consumer<Supplier<Resource>> consumer) {
        ResourceSet rs = createResourceSet();
        while (oResultSet.hasNext()) {
            OResult oResult = oResultSet.next();
            Optional<OElement> oElementOpt = oResult.getElement();
            if (oElementOpt.isPresent()) {
                OElement oElement = oElementOpt.get();
                consumer.accept(() -> {
                    EObject eObject = createEObject(rs, oElement);
                    Resource resource = rs.createResource(factory.createResourceURI(oElement));
                    resource.getContents().add(eObject);
                    populateEObject(rs, (OVertex) oElement, eObject);
                    getFactory().getEvents().fireAfterLoad(resource);
                    return resource;
                });
            }
        }
    }

    public List<Resource> query(String sql, Object... args) {
        List<Resource> result = new ArrayList<>();
        query(sql, resourceSupplier -> {
            result.add(resourceSupplier.get());
        }, args);
        return result;
    }

    public List<Resource> query(String sql, Map args) {
        List<Resource> result = new ArrayList<>();
        query(sql, resourceSupplier -> {
            result.add(resourceSupplier.get());
        }, args);
        return result;
    }

    public void query(String sql, Consumer<Supplier<Resource>> consumer, Object... args) {
        try (OResultSet rs = db.query(sql, args)) {
            getResourceList(rs, consumer);
        }
    }

    public void query(String sql, Consumer<Supplier<Resource>> consumer, Map args) {
        try (OResultSet rs = db.query(sql, args)) {
            getResourceList(rs, consumer);
        }
    }

    public Set<Resource> getSavedResources() {
        return savedResourcesMap.keySet();
    }

    public void getDependentResources(ORID orid, Consumer<Supplier<Resource>> consumer) {
        query("select distinct * from (\n" +
                "    select expand(in('EReference')) from ?\n" +
                ")\n" +
                "where @rid != ?", consumer, orid, orid);
    }

    public void getDependentResources(Resource resource, Consumer<Supplier<Resource>> consumer) {
        getDependentResources(resource.getURI(), consumer);
    }

    public void getDependentResources(URI uri, Consumer<Supplier<Resource>> consumer) {
        factory.getORIDs(uri).forEach(orid -> {
            getDependentResources(orid, consumer);
        });
    }

    public List<Resource> getDependentResources(Stream<ORID> orids) {
        List<Resource> resources = new ArrayList<>();
        orids.forEach(orid -> {
            getDependentResources(orid, resourceSupplier -> {
                resources.add(resourceSupplier.get());
            });
        });
        return resources;
    }

    public List<Resource> getDependentResources(Resource resource) {
        return getDependentResources(factory.getORIDs(resource.getURI()));
    }

    public void getAll(Consumer<Supplier<Resource>> consumer) {
        query("select from EObject", consumer);
    }

    public List<Resource> getAll() {
        List<Resource> resources = new ArrayList<>();
        getAll(resourceSupplier -> {
            resources.add(resourceSupplier.get());
        });
        return resources;
    }
}
