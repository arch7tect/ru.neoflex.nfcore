import * as React from 'react';
import {withTranslation} from 'react-i18next';
import {Button, Checkbox, Input} from "antd";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faSave} from "@fortawesome/free-regular-svg-icons";
import Ecore, {EObject, Resource} from "ecore";
import {API} from "../../../modules/api";
import {IServerQueryParam} from '../../../MainContext';

interface Props {
    closeModal?: () => void;
}

interface State {
    changeCurrent: boolean;
    accessPublic: boolean;
    componentName: string;
    allDatasetComponents: any[];
    queryFilterPattern?: EObject;
    queryAggregatePattern?: EObject;
    querySortPattern?: EObject;
    queryGroupByPattern?: EObject;
    queryCalculatedExpressionPattern?: EObject;
    /*datasetColumnPattern?: EObject;*/
    highlightPattern?: EObject;
    user?: EObject;
}

class SaveDatasetComponent extends React.Component<any, State> {

    constructor(props: any) {
        super(props);
        this.state = {
            changeCurrent: false,
            accessPublic: true,
            componentName: '',
            allDatasetComponents: []
        };
    }

    onClick(): void {
       this.saveDatasetComponentOptions();
    }

    getPattern(className:string, paramName:string) {
        API.instance().findClass('dataset', className)
            .then( (paramValue: EObject ) => {
                this.setState<never>({
                    [paramName]: paramValue
                })
            })
    };

    getAllDatasetComponents() {
        API.instance().fetchAllClasses(false).then(classes => {
            const temp = classes.find((c: Ecore.EObject) => c._id === '//DatasetComponent');
            if (temp !== undefined) {
                API.instance().findByKind(temp,  {contents: {eClass: temp.eURI()}})
                    .then((allDatasetComponents: Ecore.Resource[]) => {
                        this.setState({allDatasetComponents})
                    })
            }
        })
    };

    getUser() {
        API.instance().fetchAllClasses(false).then(classes => {
            const temp = classes.find((c: Ecore.EObject) => c._id === '//User')
            if (temp !== undefined) {
                API.instance().findByKind(temp,  {contents: {eClass: temp.eURI()}})
                    .then((users: Ecore.Resource[]) => {
                        const user = users.find( (u: Ecore.Resource) => u.eContents()[0].get('name') === this.props.context.userProfile.get('userName'))
                        this.setState({user})
                    })
            }
        })
    };

    addComponentServerParam(currentDatasetComponent: Ecore.EObject, pattern: Ecore.EObject, userProfileValue: Ecore.EObject[], paramName: string, componentName: string): void {
        currentDatasetComponent.get(componentName).clear();
        JSON.parse(userProfileValue[0].get('value'))[paramName].forEach((f: any) => {
            if (f['operation'] !== undefined) {
                let params;
                let customColumn;
                const datasetColumn = currentDatasetComponent.get('dataset').get('datasetColumn').array()
                    .filter((c: any) => c.get('name') === f['datasetColumn']);
                /*if (!datasetColumn[0]) {
                    customColumn = this.state.datasetColumnPattern?.create({
                        name:f['datasetColumn'],
                        //rdbmsDataType:"Text",
                        convertDataType:"String"
                    })
                }*/
                params = pattern.create({
                    datasetColumn: datasetColumn[0]/*||customColumn*/,
                    operation: f['operation'],
                    value: f['value'],
                    enable: f['enable'],
                    type: f['type'],
                    highlightType: f['highlightType'],
                    backgroundColor: f['backgroundColor'],
                    color: f['color']
                } as IServerQueryParam);
                currentDatasetComponent.get(componentName).add(params)
            }
        })
    };

    saveDatasetComponentOptions(): void {
        let objectId = this.props.viewObject._id;
        let params: any = {};
        if (this.props.viewObject.get('theme') !== null) {params['theme'] = this.props.viewObject.get('theme')}
        if (this.props.viewObject.get('showUniqRow') !== null) {params['showUniqRow'] = this.props.viewObject.get('showUniqRow')}
        if (this.props.viewObject.get('rowPerPage') !== null) {params['rowPerPage'] = this.props.viewObject.get('rowPerPage')}
        this.props.context.changeUserProfile(objectId, params).then (()=> {
            const userProfileDatasetView = this.props.context.userProfile.get('params').array()
                .filter( (p: any) => p.get('key') === this.props.viewObject.get('datasetView')._id);
            let currentDatasetComponent: any;
            if (userProfileDatasetView.length !== 0 &&
                JSON.parse(userProfileDatasetView[0].get('value')).name !== this.props.viewObject.get('datasetView').get('datasetComponent').get('name')) {
                let datasetComponentName = JSON.parse(userProfileDatasetView[0].get('value')).name;
                let currentDatasetComponentArr = this.state.allDatasetComponents.find( (d: Ecore.Resource) =>
                    d.eContents()[0].get('name') === datasetComponentName);
                currentDatasetComponent = currentDatasetComponentArr.eContents()[0]
            }
            else {
                currentDatasetComponent = this.props.viewObject.get('datasetView').get('datasetComponent')
            }
            currentDatasetComponent.set('access', !this.state.accessPublic ? 'Private' : 'Public');
            if (!this.state.changeCurrent) {
                currentDatasetComponent.get('audit').get('createdBy', null);
                currentDatasetComponent.get('audit').set('created', null);
                currentDatasetComponent.get('audit').set('modifiedBy', null);
                currentDatasetComponent.get('audit').set('modified', null);
            }
            const userProfileValue = this.props.context.userProfile.get('params').array()
                .filter( (p: any) => p.get('key') === currentDatasetComponent._id);
            if (userProfileValue.length !== 0) {
                this.addComponentServerParam(currentDatasetComponent, this.state.queryFilterPattern!, userProfileValue, 'serverFilters', 'serverFilter');
                this.addComponentServerParam(currentDatasetComponent, this.state.queryAggregatePattern!, userProfileValue, 'serverAggregates', 'serverAggregation');
                this.addComponentServerParam(currentDatasetComponent, this.state.querySortPattern!, userProfileValue, 'serverSorts', 'serverSort');
                this.addComponentServerParam(currentDatasetComponent, this.state.queryGroupByPattern!, userProfileValue, 'serverGroupBy', 'serverGroupBy');
                /*this.addComponentServerParam(currentDatasetComponent, this.state.queryCalculatedExpressionPattern!, userProfileValue, 'serverCalculatedExpression', 'serverCalculatedExpression');*/
                this.addComponentServerParam(currentDatasetComponent, this.state.highlightPattern!, userProfileValue, 'highlights', 'highlight');
            }
            this.props.context.changeUserProfile(currentDatasetComponent._id, undefined).then (()=> {
                const resource = currentDatasetComponent.eResource();
                if (resource) {
                    if (!this.state.changeCurrent) {
                        const contents = (eObject: EObject): EObject[] => [eObject, ...eObject.eContents().flatMap(contents)];
                        contents(resource.eContents()[0]).forEach(eObject=>{(eObject as any)._id = null});
                        resource.eContents()[0].set('name', `${this.state.componentName}`);
                        resource.set('uri', null);
                        resource.eContents()[0].set('serverFilters', `${this.state.componentName}`);
                        this.props.context.changeUserProfile(this.props.viewObject.get('datasetView')._id, {name: this.state.componentName})
                    }
                    this.saveDatasetComponent(resource);
                }
            })
         });
    }

    cloneResource(): void {
        const userProfileDatasetView = this.props.context.userProfile.get('params').array()
            .filter( (p: any) => p.get('key') === this.props.viewObject.get('datasetView')._id);
        let currentDatasetComponent: any;
        if (userProfileDatasetView.length !== 0 &&
            JSON.parse(userProfileDatasetView[0].get('value')).name !== this.props.viewObject.get('datasetView').get('datasetComponent').get('name')) {
            let datasetComponentName = JSON.parse(userProfileDatasetView[0].get('value')).name;
            let currentDatasetComponentArr = this.state.allDatasetComponents.find( (d: Ecore.Resource) =>
                d.eContents()[0].get('name') === datasetComponentName);
            currentDatasetComponent = currentDatasetComponentArr.eContents()[0]
        }
        else {currentDatasetComponent = this.props.viewObject.get('datasetView').get('datasetComponent')}
        currentDatasetComponent.set('access', !this.state.accessPublic ? 'Private' : 'Public');
        currentDatasetComponent.get('audit').get('createdBy', null);
        currentDatasetComponent.get('audit').set('created', null);
        currentDatasetComponent.get('audit').set('modifiedBy', null);
        currentDatasetComponent.get('audit').set('modified', null);
        currentDatasetComponent.set('owner', this.state.user);
        const userProfileValue = this.props.context.userProfile.get('params').array()
            .filter( (p: any) => p.get('key') === currentDatasetComponent._id);
        if (userProfileValue.length !== 0) {
            this.addComponentServerParam(currentDatasetComponent, this.state.queryFilterPattern!, userProfileValue, 'serverFilters', 'serverFilter');
            this.addComponentServerParam(currentDatasetComponent, this.state.queryAggregatePattern!, userProfileValue, 'serverAggregates', 'serverAggregation');
            this.addComponentServerParam(currentDatasetComponent, this.state.querySortPattern!, userProfileValue, 'serverSorts', 'serverSort');
            this.addComponentServerParam(currentDatasetComponent, this.state.queryGroupByPattern!, userProfileValue, 'serverGroupBy', 'serverGroupBy');
            this.addComponentServerParam(currentDatasetComponent, this.state.queryCalculatedExpressionPattern!, userProfileValue, 'serverCalculatedExpression', 'serverCalculatedExpression');
            this.addComponentServerParam(currentDatasetComponent, this.state.highlightPattern!, userProfileValue, 'highlights', 'highlight');
        }
            this.props.context.changeUserProfile(currentDatasetComponent._id, undefined).then (()=> {
                const resource = currentDatasetComponent.eResource();
                if (resource) {
                    const contents = (eObject: EObject): EObject[] => [eObject, ...eObject.eContents().flatMap(contents)];
                    contents(resource.eContents()[0]).forEach(eObject=>{(eObject as any)._id = null});
                    resource.eContents()[0].set('name', `${this.state.componentName}`);
                    resource.set('uri', null);
                    resource.eContents()[0].set('serverFilters', `${this.state.componentName}`);
                    this.props.context.changeUserProfile(this.props.viewObject.get('datasetView')._id, {name: this.state.componentName})
                    this.saveDatasetComponent(resource);
                }
            })
        this.getAllDatasetComponents()
    }

    private saveDatasetComponent(resource: Resource) {
        API.instance().saveResource(resource)
            .then((newDatasetComponent: any) => {
                this.props.closeModal!();
                this.props.viewObject.get('datasetView').set('datasetComponent', newDatasetComponent.eContents()[0]);
                const newResourceSet: Ecore.ResourceSet = this.props.viewObject.eResource().eContainer as Ecore.ResourceSet;
                const newViewObject: Ecore.EObject[] = newResourceSet.elements()
                    .filter((r: Ecore.EObject) => r.eContainingFeature.get('name') === 'view')
                    .filter((r: Ecore.EObject) => r.eContainingFeature._id === this.props.context.viewObject.eContainingFeature._id)
                    .filter((r: Ecore.EObject) => r.eContainer.get('name') === this.props.context.viewObject.eContainer.get('name'))
                this.props.context.updateContext!(({viewObject: newViewObject[0]}));
                if (!this.state.changeCurrent) {
                    this.getAllDatasetComponents()
                }
            });
    }

    onChangeName(e: any): void {
        this.setState({componentName: e})
    }

    onChangeCurrent(): void {
        this.state.changeCurrent ? this.setState({changeCurrent: false}) : this.setState({changeCurrent: true})
    }

    onChangeAccess(): void {
        this.state.accessPublic ? this.setState({accessPublic: false}) : this.setState({accessPublic: true})
    }

    componentDidMount(): void {
        if (this.state.allDatasetComponents.length === 0) {this.getAllDatasetComponents()}
        if (!this.state.queryFilterPattern) this.getPattern('QueryFilter', 'queryFilterPattern');
        if (!this.state.queryAggregatePattern) this.getPattern('QueryAggregate', 'queryAggregatePattern');
        if (!this.state.querySortPattern) this.getPattern('QuerySort', 'querySortPattern');
        if (!this.state.queryGroupByPattern) this.getPattern('QueryGroupBy', 'queryGroupByPattern');
        if (!this.state.queryCalculatedExpressionPattern) this.getPattern('QueryCalculatedExpression', 'queryCalculatedExpressionPattern');
        if (!this.state.highlightPattern) this.getPattern('Highlight', 'highlightPattern');
        /*if (!this.state.datasetColumnPattern) this.getPattern('DatasetColumn', 'datasetColumnPattern');*/
        if (!this.state.user) this.getUser();
    }

    render() {
        const { t } = this.props;

        return (
            <div>
                <Input
                    placeholder={'new report name'}
                    disabled={this.state.changeCurrent}
                    style={{ width: '200px', marginRight: '10px', marginBottom: '20px'}}
                    allowClear={true}
                    onChange={(e: any) => this.onChangeName(e.target.value)}
                />
                <Checkbox
                    checked={this.state.changeCurrent}
                    disabled={false}
                    onChange={() => this.onChangeCurrent()}
                >
                    Change current
                </Checkbox>
                <Checkbox
                    checked={this.state.accessPublic}
                    disabled={false}
                    onChange={() => this.onChangeAccess()}
                >
                    Public
                </Checkbox>
                <div>
                <Button title={t('save')} style={{ width: '100px', color: 'rgb(151, 151, 151)'}} onClick={() => this.onClick()}>
                    <FontAwesomeIcon icon={faSave} size='1x'/>
                </Button>
                </div>
            </div>

        )
    }
}

export default withTranslation()(SaveDatasetComponent)
