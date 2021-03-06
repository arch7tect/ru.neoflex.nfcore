package ru.neoflex.nfcore.base.components;

import org.springframework.stereotype.Component;
import ru.neoflex.nfcore.base.auth.AuthPackage;
import ru.neoflex.nfcore.base.auth.impl.AuthFactoryExt;
import ru.neoflex.nfcore.base.scheduler.SchedulerPackage;
import ru.neoflex.nfcore.base.scheduler.impl.SchedulerFactoryExt;
import ru.neoflex.nfcore.base.supply.SupplyPackage;
import ru.neoflex.nfcore.base.tag.impl.TagFactoryExt;
import ru.neoflex.nfcore.base.tag.impl.TagValidatorExt;
import ru.neoflex.nfcore.base.types.TypesPackage;
import ru.neoflex.nfcore.base.tag.TagPackage;

import javax.annotation.PostConstruct;

@Component
public class BaseModuleRegistry extends ModuleRegistryImpl {
    @PostConstruct
    void init() {
        registerEPackage(TypesPackage.eINSTANCE);
        registerEPackage(AuthPackage.eNS_URI, ()->AuthPackage.eINSTANCE, new AuthFactoryExt());
        registerEPackage(SchedulerPackage.eNS_URI, ()->SchedulerPackage.eINSTANCE, new SchedulerFactoryExt());
        registerEPackage(SupplyPackage.eINSTANCE);
        registerEPackage(TagPackage.eNS_URI,()-> TagPackage.eINSTANCE, new TagFactoryExt(), new TagValidatorExt());
    }
}
