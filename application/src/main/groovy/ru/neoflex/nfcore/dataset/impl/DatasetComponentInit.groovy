package ru.neoflex.nfcore.dataset.impl

import org.eclipse.emf.ecore.EClass
import ru.neoflex.nfcore.application.ApplicationFactory
import ru.neoflex.nfcore.base.auth.AuthPackage
import ru.neoflex.nfcore.base.services.Context
import ru.neoflex.nfcore.base.util.DocFinder
import ru.neoflex.nfcore.dataset.Access
import ru.neoflex.nfcore.dataset.DataType
import ru.neoflex.nfcore.dataset.DatasetFactory
import ru.neoflex.nfcore.dataset.DatasetPackage
import ru.neoflex.nfcore.dataset.DatasetComponent
import ru.neoflex.nfcore.dataset.Filter
import ru.neoflex.nfcore.dataset.JdbcDataset
import ru.neoflex.nfcore.dataset.Operations

class DatasetComponentInit {
    static def findOrCreateEObject(EClass eClass, String name) {
        def resources = DocFinder.create(Context.current.store, eClass, [name: name])
                .execute().resourceSet
        return resources.resources.get(0).contents.get(0)
    }

    static def createDatasetComponent(String name, String JdbcDataset) {
        def rs = DocFinder.create(Context.current.store, DatasetPackage.Literals.DATASET_COMPONENT, [name: name])
                .execute().resourceSet
        if (rs.resources.empty) {
            def datasetComponent = DatasetFactory.eINSTANCE.createDatasetComponent()
            datasetComponent.name = name
            def owner = findOrCreateEObject(AuthPackage.Literals.USER, 'admin')
            datasetComponent.setOwner(owner)
            datasetComponent.access = Access.DEFAULT
            def dataset = findOrCreateEObject(DatasetPackage.Literals.JDBC_DATASET, JdbcDataset)
            if (dataset) {
                datasetComponent.setDataset(dataset)
                datasetComponent.useServerFilter = true
            }
            rs.resources.add(Context.current.store.createEObject(datasetComponent))
            return rs.resources.get(0).contents.get(0) as DatasetComponent
        }
//        else if ((rs.resources.get(0).contents.get(0) as DatasetComponent).dataset == null) {
//            def datasetComponentRef = Context.current.store.getRef(rs.resources.get(0))
//            def datasetComponent = rs.resources.get(0).contents.get(0) as DatasetComponent
//            def dataset = findOrCreateEObject(DatasetPackage.Literals.JDBC_DATASET, JdbcDataset)
//            def owner = findOrCreateEObject(AuthPackage.Literals.USER, 'admin')
//            datasetComponent.setOwner(owner)
//            datasetComponent.access = Access.DEFAULT
//            if (dataset) {
//                datasetComponent.setDataset(dataset)
//                datasetComponent.useServerFilter = true
//            }
//            Context.current.store.updateEObject(datasetComponentRef, datasetComponent)
//        }
    }

    static def createAllColumn(String name) {
        def rs = DocFinder.create(Context.current.store, DatasetPackage.Literals.DATASET_COMPONENT, [name: name])
                .execute().resourceSet
        if (!rs.resources.empty && (rs.resources.get(0).contents.get(0) as DatasetComponent).column.size() == 0) {
            def datasetComponentRef = Context.current.store.getRef(rs.resources.get(0))
            def datasetComponent = rs.resources.get(0).contents.get(0) as DatasetComponent
            if (datasetComponent.dataset) {
                if (datasetComponent.dataset.datasetColumn.size() != 0) {
                    def columns = datasetComponent.dataset.datasetColumn
                    for (int i = 0; i <= columns.size() - 1; ++i) {
                        def rdbmsColumn = DatasetFactory.eINSTANCE.createRdbmsColumn()
                        rdbmsColumn.name = columns[i].name
                        rdbmsColumn.datasetColumn = columns[i]
                        def typography = ApplicationFactory.eINSTANCE.createTypography()
                        typography.name = columns[i].name
                                .replace("incomedate","дата")
                                .replace("branch","филиал")
                                .replace("income","доход")
                        rdbmsColumn.headerName = typography
                        rdbmsColumn.sortable = true
                        rdbmsColumn.resizable = true
                        rdbmsColumn.headerTooltip = "type: " + columns[i].convertDataType
                        rdbmsColumn.filter = columns[i].convertDataType == DataType.DATE || columns[i].convertDataType == DataType.TIMESTAMP
                                ? Filter.DATE_COLUMN_FILTER :
                                columns[i].convertDataType == DataType.INTEGER || columns[i].convertDataType == DataType.DECIMAL
                                        ? Filter.NUMBER_COLUMN_FILTER : Filter.TEXT_COLUMN_FILTER
                        datasetComponent.column.each { c->
                            if (c.name == columns[i].name.toString()) {
                                throw new IllegalArgumentException("Please, change your query in Dataset. It has similar column`s name")
                            }
                        }
                        datasetComponent.column.add(rdbmsColumn)
                    }
                }
            }
            Context.current.store.updateEObject(datasetComponentRef, datasetComponent)
        }
    }

    static def createServerFilters(String name, String JdbcDataset) {
        def rs = DocFinder.create(Context.current.store, DatasetPackage.Literals.DATASET_COMPONENT, [name: name])
                .execute().resourceSet
        if (!rs.resources.empty && (rs.resources.get(0).contents.get(0) as DatasetComponent).serverFilter.size() == 0) {
            def datasetComponentRef = Context.current.store.getRef(rs.resources.get(0))
            def datasetComponent = rs.resources.get(0).contents.get(0) as DatasetComponent

            def dataset = findOrCreateEObject(DatasetPackage.Literals.JDBC_DATASET, JdbcDataset) as JdbcDataset

            def serverFilter1 = DatasetFactory.eINSTANCE.createQueryFilter()
            /*def datasetColumn1 = dataset.datasetColumn.find { c -> c.name == "e_id"}
            serverFilter1.setDatasetColumn(datasetColumn1)*/
            serverFilter1.datasetColumn = "e_id"
            serverFilter1.operation = Operations.LESS_THAN
            serverFilter1.value = 100000
            serverFilter1.enable = true
            datasetComponent.serverFilter.add(serverFilter1)

            def serverFilter2 = DatasetFactory.eINSTANCE.createQueryFilter()
            /*def datasetColumn2 = dataset.datasetColumn.find { c -> c.name == "e_id"}
            serverFilter2.setDatasetColumn(datasetColumn2)*/
            serverFilter2.datasetColumn = "e_id"
            serverFilter2.operation = Operations.LESS_THAN
            serverFilter2.value = 4000
            serverFilter2.enable = false
            datasetComponent.serverFilter.add(serverFilter2)

            def serverFilter3 = DatasetFactory.eINSTANCE.createQueryFilter()
            /*def datasetColumn3 = dataset.datasetColumn.find { c -> c.name == "name"}
            serverFilter3.setDatasetColumn(datasetColumn3)*/
            serverFilter3.datasetColumn = "e_id"
            serverFilter3.operation = Operations.INCLUDE_IN
            serverFilter3.value = "test"
            serverFilter3.enable = true
            datasetComponent.serverFilter.add(serverFilter3)

            datasetComponent.useServerFilter = true

            Context.current.store.updateEObject(datasetComponentRef, datasetComponent)
        }
    }

    static def createAllColumnNRDemoMain(String name) {
        def rs = DocFinder.create(Context.current.store, DatasetPackage.Literals.DATASET_COMPONENT, [name: name])
                .execute().resourceSet
        if (!rs.resources.empty && (rs.resources.get(0).contents.get(0) as DatasetComponent).column.size() == 0) {
            def datasetComponentRef = Context.current.store.getRef(rs.resources.get(0))
            def datasetComponent = rs.resources.get(0).contents.get(0) as DatasetComponent
            if (datasetComponent.dataset) {
                if (datasetComponent.dataset.datasetColumn.size() != 0) {
                    def columns = datasetComponent.dataset.datasetColumn
                    for (int i = 0; i <= columns.size() - 1; ++i) {
                        def rdbmsColumn = DatasetFactory.eINSTANCE.createRdbmsColumn()
                        rdbmsColumn.name = columns[i].name
                        rdbmsColumn.datasetColumn = columns[i]
                        def typography = ApplicationFactory.eINSTANCE.createTypography()
                        typography.name = columns[i].name
                                .replace("ON_DATE","Отчётная дата")
                                .replace("ROW_NUMBER","Номер строки")
                                .replace("F110_CODE","Код обозначения расшифровки")
                                .replace("AMOUNT_RUB","Сумма в рублях")
                                .replace("AMOUNT_CUR","Сумма в иностранной валюте")
                        rdbmsColumn.headerName = typography
                        rdbmsColumn.sortable = true
                        rdbmsColumn.resizable = true
                        if (rdbmsColumn.name in ["ON_DATE"]) {
                            rdbmsColumn.hide = true
                        }
                        rdbmsColumn.headerTooltip = "type: " + columns[i].convertDataType
                        rdbmsColumn.filter = columns[i].convertDataType == DataType.DATE || columns[i].convertDataType == DataType.TIMESTAMP
                                ? Filter.DATE_COLUMN_FILTER :
                                columns[i].convertDataType == DataType.INTEGER || columns[i].convertDataType == DataType.DECIMAL
                                        ? Filter.NUMBER_COLUMN_FILTER : Filter.TEXT_COLUMN_FILTER
                        datasetComponent.column.each { c->
                            if (c.name == columns[i].name.toString()) {
                                throw new IllegalArgumentException("Please, change your query in Dataset. It has similar column`s name")
                            }
                        }
                        if (rdbmsColumn.name in ["ROW_NUMBER","F110_CODE","AMOUNT_RUB"]) {
                            datasetComponent.column.add(rdbmsColumn)
                        }
                        if (rdbmsColumn.name in ["AMOUNT_CUR"] && name == "DatasetNRDemoSection4") {
                            datasetComponent.column.add(rdbmsColumn)
                        }
                    }
                }
            }
            Context.current.store.updateEObject(datasetComponentRef, datasetComponent)
        }
    }

    static def createServerFiltersNRDemo(String name, String JdbcDataset) {
        def rs = DocFinder.create(Context.current.store, DatasetPackage.Literals.DATASET_COMPONENT, [name: name])
                .execute().resourceSet
        if (!rs.resources.empty && (rs.resources.get(0).contents.get(0) as DatasetComponent).serverFilter.size() == 0) {
            def datasetComponentRef = Context.current.store.getRef(rs.resources.get(0))
            def datasetComponent = rs.resources.get(0).contents.get(0) as DatasetComponent

            def serverFilter1 = DatasetFactory.eINSTANCE.createQueryFilter()
            /*def datasetColumn1 = dataset.datasetColumn.find { c -> c.name == "e_id"}
            serverFilter1.setDatasetColumn(datasetColumn1)*/
            serverFilter1.datasetColumn = "REPORT_PRECISION"
            serverFilter1.operation = Operations.EQUAL_TO
            serverFilter1.value = 1000
            serverFilter1.enable = true
            datasetComponent.serverFilter.add(serverFilter1)

            datasetComponent.useServerFilter = true

            Context.current.store.updateEObject(datasetComponentRef, datasetComponent)
        }
    }


    static def createServerFiltersNRDemoDetail(String name, String JdbcDataset) {
        def rs = DocFinder.create(Context.current.store, DatasetPackage.Literals.DATASET_COMPONENT, [name: name])
                .execute().resourceSet
        if (!rs.resources.empty && (rs.resources.get(0).contents.get(0) as DatasetComponent).serverFilter.size() == 0) {
            def datasetComponentRef = Context.current.store.getRef(rs.resources.get(0))
            def datasetComponent = rs.resources.get(0).contents.get(0) as DatasetComponent

            def serverFilter1 = DatasetFactory.eINSTANCE.createQueryFilter()
            /*def datasetColumn1 = dataset.datasetColumn.find { c -> c.name == "e_id"}
            serverFilter1.setDatasetColumn(datasetColumn1)*/
            serverFilter1.datasetColumn = "SECTION_NUMBER"
            serverFilter1.operation = Operations.EQUAL_TO
            serverFilter1.value = 1
            serverFilter1.enable = true
            datasetComponent.serverFilter.add(serverFilter1)

            datasetComponent.useServerFilter = true

            Context.current.store.updateEObject(datasetComponentRef, datasetComponent)
        }
    }

    static def createAllColumnNRDemoDetail(String name) {
        def rs = DocFinder.create(Context.current.store, DatasetPackage.Literals.DATASET_COMPONENT, [name: name])
                .execute().resourceSet
        if (!rs.resources.empty && (rs.resources.get(0).contents.get(0) as DatasetComponent).column.size() == 0) {
            def datasetComponentRef = Context.current.store.getRef(rs.resources.get(0))
            def datasetComponent = rs.resources.get(0).contents.get(0) as DatasetComponent
            if (datasetComponent.dataset) {
                if (datasetComponent.dataset.datasetColumn.size() != 0) {
                    def columns = datasetComponent.dataset.datasetColumn
                    for (int i = 0; i <= columns.size() - 1; ++i) {
                        def rdbmsColumn = DatasetFactory.eINSTANCE.createRdbmsColumn()
                        rdbmsColumn.name = columns[i].name
                        rdbmsColumn.datasetColumn = columns[i]
                        def typography = ApplicationFactory.eINSTANCE.createTypography()
                        typography.name = columns[i].name
                                .replace("SECTION_NUMBER","Раздел")
                                .replace("ROW_NUMBER","Номер строки")
                                .replace("F110_CODE","Код обозначения расшифровки")
                                .replace("ACCOUNT_NUMBER","Номер л/c")
                                .replace("F102_SYMBOL","Символ 102 формы")
                                .replace("ACCOUNT_AMOUNT_RUB","Остаток на счёте, руб.")
                                .replace("AMOUNT_RUB","Сумма, руб")
                                .replace("ACCOUNT_NAME","Наименование л/c")
                                .replace("OPTION_PREMIUM_AMOUNT","Сумма опционной премии")
                                .replace("CUSTOMER_NAME","Наименование клиента по счёту")
                                .replace("PARTY_TYPE","Тип клиента")
                                .replace("IS_CO","Клиент по счёту является КО")
                                .replace("IS_RESIDENT","Клиент по счёту является резидентом")
                                .replace("AGREEMENT_NUMBER","Номер договора/сделки")
                                .replace("ACTIVE_RESERVE_TYPE","Тип резервирования актива")
                        rdbmsColumn.headerName = typography
                        rdbmsColumn.sortable = true
                        rdbmsColumn.resizable = true
                        if (rdbmsColumn.name in ["SECTION_NUMBER","ROW_NUMBER"]) {
                            rdbmsColumn.hide = true
                        }
                        rdbmsColumn.headerTooltip = "type: " + columns[i].convertDataType
                        rdbmsColumn.filter = columns[i].convertDataType == DataType.DATE || columns[i].convertDataType == DataType.TIMESTAMP
                                ? Filter.DATE_COLUMN_FILTER :
                                columns[i].convertDataType == DataType.INTEGER || columns[i].convertDataType == DataType.DECIMAL
                                        ? Filter.NUMBER_COLUMN_FILTER : Filter.TEXT_COLUMN_FILTER
                        datasetComponent.column.each { c->
                            if (c.name == columns[i].name.toString()) {
                                throw new IllegalArgumentException("Please, change your query in Dataset. It has similar column`s name")
                            }
                        }
                        if (rdbmsColumn.name in ["SECTION_NUMBER","ROW_NUMBER","F110_CODE","ACCOUNT_NUMBER","F102_SYMBOL","AMOUNT_RUB",
                            "ACCOUNT_NAME","ACCOUNT_AMOUNT_RUB","OPTION_PREMIUM_AMOUNT","CUSTOMER_NAME","PARTY_TYPE","IS_CO",
                            "IS_RESIDENT","AGREEMENT_NUMBER","ACTIVE_RESERVE_TYPE"]) {
                            datasetComponent.column.add(rdbmsColumn)
                        }
                    }
                }
            }
            Context.current.store.updateEObject(datasetComponentRef, datasetComponent)
        }
    }

    static def createAllColumnNRDemoCalcMart(String name) {
        def rs = DocFinder.create(Context.current.store, DatasetPackage.Literals.DATASET_COMPONENT, [name: name])
                .execute().resourceSet
        if (!rs.resources.empty && (rs.resources.get(0).contents.get(0) as DatasetComponent).column.size() == 0) {
            def datasetComponentRef = Context.current.store.getRef(rs.resources.get(0))
            def datasetComponent = rs.resources.get(0).contents.get(0) as DatasetComponent
            if (datasetComponent.dataset) {
                if (datasetComponent.dataset.datasetColumn.size() != 0) {
                    def columns = datasetComponent.dataset.datasetColumn
                    for (int i = 0; i <= columns.size() - 1; ++i) {
                        def rdbmsColumn = DatasetFactory.eINSTANCE.createRdbmsColumn()
                        rdbmsColumn.name = columns[i].name
                        rdbmsColumn.datasetColumn = columns[i]
                        def typography = ApplicationFactory.eINSTANCE.createTypography()
                        typography.name = columns[i].name
                                .replace("EVENT_PARAMETERS_LIST","Расчетный период")
                                .replace("PARTITION_KEY","Вид расчета")
                                .replace("BEGIN_DATE","Дата начала")
                                .replace("END_DATE","Дата окончания")
                                .replace("STATUS","Статус")
                        rdbmsColumn.headerName = typography
                        rdbmsColumn.sortable = true
                        rdbmsColumn.resizable = true
                        if (rdbmsColumn.name in ["RECORD_ID"]) {
                            rdbmsColumn.hide = true
                        }
                        rdbmsColumn.headerTooltip = "type: " + columns[i].convertDataType
                        rdbmsColumn.filter = columns[i].convertDataType == DataType.DATE || columns[i].convertDataType == DataType.TIMESTAMP
                                ? Filter.DATE_COLUMN_FILTER :
                                columns[i].convertDataType == DataType.INTEGER || columns[i].convertDataType == DataType.DECIMAL
                                        ? Filter.NUMBER_COLUMN_FILTER : Filter.TEXT_COLUMN_FILTER
                        datasetComponent.column.each { c->
                            if (c.name == columns[i].name.toString()) {
                                throw new IllegalArgumentException("Please, change your query in Dataset. It has similar column`s name")
                            }
                        }
                        datasetComponent.column.add(rdbmsColumn)
                    }
                }
            }
            Context.current.store.updateEObject(datasetComponentRef, datasetComponent)
        }
    }

    static def createAllColumnNRDemoKliko(String name) {
        def rs = DocFinder.create(Context.current.store, DatasetPackage.Literals.DATASET_COMPONENT, [name: name])
                .execute().resourceSet
        if (!rs.resources.empty && (rs.resources.get(0).contents.get(0) as DatasetComponent).column.size() == 0) {
            def datasetComponentRef = Context.current.store.getRef(rs.resources.get(0))
            def datasetComponent = rs.resources.get(0).contents.get(0) as DatasetComponent
            if (datasetComponent.dataset) {
                if (datasetComponent.dataset.datasetColumn.size() != 0) {
                    def columns = datasetComponent.dataset.datasetColumn
                    for (int i = 0; i <= columns.size() - 1; ++i) {
                        def rdbmsColumn = DatasetFactory.eINSTANCE.createRdbmsColumn()
                        rdbmsColumn.name = columns[i].name
                        rdbmsColumn.datasetColumn = columns[i]
                        def typography = ApplicationFactory.eINSTANCE.createTypography()
                        typography.name = columns[i].name
                                .replace("ON_DATE","Отчетная дата (на)")
                                .replace("FORM_TYPE","Тип формы")
                                .replace("FILE_NAME","Имя файла")
                                .replace("FILE_STATUS","Результат")
                                .replace("DATE_BEGIN","Дата начала")
                                .replace("DATE_END","Дата окончания")
                                .replace("FILE_SIZE","Размер (байт)")
                                .replace("MESSAGE","Message")
                                .replace("STATUS_CB","Status cb")
                        rdbmsColumn.headerName = typography
                        rdbmsColumn.sortable = true
                        rdbmsColumn.resizable = true
                        if (rdbmsColumn.name in ["FILE_ID","SPOD_DATE","INCLUDE_SPOD","BRANCH_RK","BRANCH_CODE","MESSAGE","STATUS_CB"]) {
                            rdbmsColumn.hide = true
                        }
                        rdbmsColumn.headerTooltip = "type: " + columns[i].convertDataType
                        rdbmsColumn.filter = columns[i].convertDataType == DataType.DATE || columns[i].convertDataType == DataType.TIMESTAMP
                                ? Filter.DATE_COLUMN_FILTER :
                                columns[i].convertDataType == DataType.INTEGER || columns[i].convertDataType == DataType.DECIMAL
                                        ? Filter.NUMBER_COLUMN_FILTER : Filter.TEXT_COLUMN_FILTER
                        datasetComponent.column.each { c->
                            if (c.name == columns[i].name.toString()) {
                                throw new IllegalArgumentException("Please, change your query in Dataset. It has similar column`s name")
                            }
                        }
                        datasetComponent.column.add(rdbmsColumn)
                    }
                }
            }
            Context.current.store.updateEObject(datasetComponentRef, datasetComponent)
        }
    }

    DatasetComponentInit() {}

}

