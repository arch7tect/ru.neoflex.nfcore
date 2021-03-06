package ru.neoflex.nfcore.base;

import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.neoflex.nfcore.base.auth.*;
import ru.neoflex.nfcore.base.services.Context;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"dbtype=orientdb", "orientdb.dbname=modelstest"})
public class StoreTests {
    @Autowired
    Context context;

    @Test
    public void loadAndStore() throws Exception {
        context.getStore().inTransaction(false, tx -> {
            Role superAdminRole = createSuperAdminRole();
            User superAdminUser = createSuperAdminUser();
            superAdminUser.getRoles().add(superAdminRole);
            Resource roleResource = context.getStore().createEObject(superAdminRole);
            Resource userResource = context.getStore().createEObject(superAdminUser);
            Resource resource2 = context.getStore().createResourceSet().createResource(userResource.getURI());
            resource2.load(null);
            Assert.assertEquals(superAdminUser.getName(), ((User)resource2.getContents().get(0)).getName());
            context.getStore().deleteResource(userResource.getURI());
            context.getStore().deleteResource(roleResource.getURI());
            return 0;
        });
    }

    public static Role createSuperAdminRole() {
        Role superAdmin = AuthFactory.eINSTANCE.createRole();
        superAdmin.setName("SuperAdminRole_FORTEST2");
        Permission allPermission = AuthFactory.eINSTANCE.createAllPermission();
        allPermission.setGrantType(GrantType.WRITE);
        superAdmin.getGrants().add(allPermission);
        return superAdmin;
    }

    public static User createSuperAdminUser() {
        User superAdminUser = AuthFactory.eINSTANCE.createUser();
        superAdminUser.setName("SuperAdminUser_FORTEST2");
        superAdminUser.setEmail("admin@neoflex.ru");
        PasswordAuthenticator password = AuthFactory.eINSTANCE.createPasswordAuthenticator();
        password.setPassword("secret");
        password.setDisabled(false);
        superAdminUser.getAuthenticators().add(password);
        return superAdminUser;
    }

}
