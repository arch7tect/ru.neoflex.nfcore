import React, { Fragment, useState } from 'react';
import { Modal, Dropdown, Menu, Button, Select } from 'antd'
import Ecore from 'ecore';
import i18next from 'i18next';

import { API } from './../modules/api' 
import FormComponentMapper from './FormComponentMapper';

interface Props {
    translate: i18next.TFunction,
    mainEObject: Ecore.EObject
}

export default function Operations(props: Props): JSX.Element {

    const t  = props.translate
    const [ paramModalVisible, setParamModalVisible ] = useState<boolean>(false)
    const [ refModalVisible, setRefModalVisible ] = useState<boolean>(false)
    const [ selectedRefUries, setSelectedRefUries ] = useState<string[]>([])
    const [ addRefPossibleTypes, setAddRefPossibleTypes ] = useState<string[]>([])
    const [ addRefProperty, setAddRefProperty ] = useState<string>('')
    const [ targetOperationObject, setTargetOperationObject ] = useState<Ecore.EObject|null>(null)
    const [ parameters, setParameters ] = useState<Object>({})
    const [ methodName, setMethodName ] = useState<string>('')

    function runAction(methodName: string) {
        if(methodName){
            const ref = `${props.mainEObject.eResource().get('uri')}?rev=${props.mainEObject.eResource().rev}`;
            API.instance().call(ref, methodName, Object.values(parameters)).then(result => 
                console.log(result)
            )
            setParamModalVisible(false)
        }
    }

    function onMenuSelect(e:any){
        const operations = props.mainEObject.eClass.get('eOperations').array()
        const targetOperationObject = operations.find((op:any) => op.get('name') === e.key)
        if(targetOperationObject && targetOperationObject.get('eParameters').size() > 0){
            setParamModalVisible(true)
            setTargetOperationObject(targetOperationObject)
            setMethodName(e.key)
        } else {
            runAction(e.key)
        }
    }

    function handleDeleteRef(deletedObject: any, propertyName: string) {
        //TODO: test!
        console.log(deletedObject)
        const paramList:{[key: string]: any} = parameters
        const filteredParameters = paramList[propertyName].filter((refObj: any) => refObj.$ref !== deletedObject.$ref)
        setParameters({ ...parameters, [propertyName]: filteredParameters })
    }

    function handleDeleteSingleRef(deletedObject: any, propertyName: string) {
        //TODO: test!
        const paramList: {[key: string]: any} = { ...parameters }
        delete paramList[propertyName]
        setParameters(paramList)
    }

    function handleAddNewRef() {
        const resources: any = []
        let refsArray: Array<Object> = []
        props.mainEObject.eResource().eContainer.get('resources').each((res: { [key: string]: any }) => {
            const isFound = selectedRefUries.indexOf(res.eURI() as never)
            isFound !== -1 && resources.push(res)
        })

        if (resources.length > 0) {
            if (targetOperationObject!.get('upperBound') === -1) {
                resources.forEach((res:any) => {
                    refsArray.push({
                        $ref: res.eContents()[0].eURI(),
                        eClass: res.eContents()[0].eClass.eURI()
                    })
                })
                setParameters({
                    ...parameters, 
                    [addRefProperty]: refsArray
                })
            } else {
                const firstResource = resources.find((res: Ecore.Resource) => res.eURI() === selectedRefUries[0])
                //if a user choose several resources for the adding, but upperBound === 1, we put only first resource
                setParameters({
                    ...parameters, 
                    [addRefProperty]: {
                        $ref: firstResource!.eContents()[0].eURI(),
                        eClass: firstResource!.eContents()[0].eClass.eURI()
                    } 
                })
            }
        }
        setRefModalVisible(false)
    }

    function onBrowse(EObject: Ecore.EObject){
        const addRefPossibleTypes = []
        addRefPossibleTypes.push(EObject.get('eType').get('name'))
        EObject.get('eType').get('eAllSubTypes').forEach((subType: Ecore.EObject) =>
            addRefPossibleTypes.push(subType.get('name'))
        )
        setRefModalVisible(true)
        setAddRefProperty(EObject.get('name'))
        setAddRefPossibleTypes(addRefPossibleTypes)
    }

    function onParameterChange(newValue: any, componentName: string, targetObject: any, EObject: Ecore.EObject){
        if (componentName === 'DatePickerComponent') {
            newValue = newValue ? newValue.format() : ''
        }
        setParameters({...parameters, [EObject.get('name')]: newValue })
    }

    function renderParameters() {
        const paramList: { [key: string]: any } = parameters
        return (
            targetOperationObject!.get('eParameters').map((param: Ecore.EObject, idx: number) => {
                const component = FormComponentMapper.getComponent({
                    idx: idx,
                    ukey: 'run',
                    eObject: param,
                    eType: param.get('eType'),
                    id: param.get('name'),
                    onChange: onParameterChange,
                    value: paramList[param.get('name')],
                    upperBound: targetOperationObject!.get('upperBound'),
                    mainEObject: props.mainEObject,
                    onBrowse: onBrowse,
                    onEClassBrowse: onBrowse,
                    handleDeleteSingleRef: handleDeleteSingleRef,
                    handleDeleteRef: handleDeleteRef,
                })
                return (
                    <div style={{ marginBottom: '5px' }}>
                        <div style={{ marginBottom: '5px' }}>{param.get('name')}</div>{component}
                    </div>
                )
            })
        )
    }

    function renderMenu(){
        const menu = () => {
            return <Menu onClick={onMenuSelect}> 
                {props.mainEObject.eClass.get('eOperations').map((oper: Ecore.EObject)=>{
                    return <Menu.Item key={oper.get('name')}>
                        {oper.get('name')}
                    </Menu.Item>
                })}
            </Menu>
        }

        return <Dropdown placement="bottomCenter" overlay={menu}>
            <Button className="panel-button" icon="bulb" />
        </Dropdown>
    }

    return (
        <Fragment>
            {props.mainEObject.eClass && props.mainEObject.eClass.get('eOperations').size() > 0 && renderMenu()}
            {paramModalVisible && <Modal
                key="run_param_modal"
                width={'500px'}
                title={t('runparameters')}
                visible={paramModalVisible}
                onCancel={()=>setParamModalVisible(false)}
                onOk={() => {
                    setParamModalVisible(false)
                    runAction(methodName)
                }}
            >
                {renderParameters()}
            </Modal>}
            {refModalVisible && <Modal
                    key="add_ref_modal"
                    width={'700px'}
                    title={t('addreference')}
                    visible={refModalVisible}
                    onCancel={()=>setRefModalVisible(false)}
                    footer={selectedRefUries.length > 0 ? 
                        <Button type="primary" onClick={handleAddNewRef}>OK</Button>: null}
                >
                    <Select
                        mode="multiple"
                        style={{ width: '100%' }}
                        placeholder="Please select"
                        defaultValue={[]}
                        onChange={(uriArray: string[]) => {
                            setSelectedRefUries(uriArray)
                        }}
                    >
                        {props.mainEObject.eClass && props.mainEObject.eResource().eContainer.get('resources').map((res: { [key: string]: any }, index: number) => {
                            const possibleTypes: Array<string> = addRefPossibleTypes
                            const isEObjectType: boolean = possibleTypes[0] === 'EObject'
                            return isEObjectType ?
                                <Select.Option key={index} value={res.eURI()}>
                                    {<b>
                                        {`${res.eContents()[0].eClass.get('name')}`}
                                    </b>}
                                    &nbsp;
                                    {`${res.eContents()[0].get('name')}`}
                                </Select.Option>
                                :
                                possibleTypes.includes(res.eContents()[0].eClass.get('name')) && <Select.Option key={index} value={res.eURI()}>
                                    {<b>
                                        {`${res.eContents()[0].eClass.get('name')}`}
                                    </b>}
                                    &nbsp;
                                    {`${res.eContents()[0].get('name')}`}
                                </Select.Option>
                        })}
                    </Select>
                </Modal>}
        </Fragment>
    )

}