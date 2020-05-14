package ru.neoflex.nfcore.masterdata.impl

import ru.neoflex.nfcore.base.components.SpringContext
import ru.neoflex.nfcore.masterdata.services.MasterdataProvider

class EntityTypeExt extends EntityTypeImpl {
    public void activate() {
        MasterdataProvider provider = SpringContext.getBean(MasterdataProvider.class);
        if (provider != null) provider.activateEntityType(this)
    }

    public void deactivate(boolean deleteTables) {
        MasterdataProvider provider = SpringContext.getBean(MasterdataProvider.class);
        if (provider != null) provider.deactivateEntityType(this, deleteTables)
    }


}