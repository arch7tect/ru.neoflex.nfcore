import * as React from 'react';
import {WithTranslation, withTranslation} from 'react-i18next';
import {EObject} from 'ecore';
import {Button, Row, Col, Form, Select, Switch} from 'antd';
import {FormComponentProps} from "antd/lib/form";
import {faPlay, faPlus, faRedo, faTrash} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {paramType} from "./DatasetView"
import {IServerQueryParam} from "../../../MainContext";
import {SortableContainer, SortableElement} from 'react-sortable-hoc';
import '../../../styles/Draggable.css';
import arrayMove from "array-move";

interface Props {
    parametersArray?: Array<IServerQueryParam>;
    columnDefs?:  Array<any>;
    onChangeParameters?: (newServerParam: any[], paramName: paramType) => void;
    saveChanges?: (newServerParam: any[], paramName: paramType) => void;
    isVisible?: boolean;
    componentType?: paramType;
}

interface State {
    parametersArray: IServerQueryParam[] | undefined;
    backgroundColorVisible?: boolean;
    textColorVisible?: boolean;
    colorIndex?: any;
    color?: any;
}

export class DrawerParameterComponent<T extends Props, V extends State> extends React.Component<Props & FormComponentProps & WithTranslation & any, State> {
    t: any;
    getFieldDecorator: any;
    paramNotification: string;

    constructor(props: any) {
        super(props);
        this.state = {
            parametersArray: this.props.parametersArray,
        };
        this.handleChange = this.handleChange.bind(this);
        this.t = this.props.t;
        this.getFieldDecorator = this.props.form?.getFieldDecorator;
        switch (this.props.componentType) {
            case paramType.sort:
                this.paramNotification = "Sort notification";
                break;
            case paramType.highlights:
                this.paramNotification = "Highlight notification";
                break;
            case paramType.aggregate:
                this.paramNotification = "Aggregate notification";
                break;
            case paramType.filter:
                this.paramNotification = "Filter notification";
                break;
            case paramType.group:
                this.paramNotification = "Group by notification";
                break;
            default:
                this.paramNotification = "Param notification"
        }
    }

    componentDidUpdate(prevProps: Readonly<any>, prevState: Readonly<any>, snapshot?: any): void {
        if (JSON.stringify(prevProps.isVisible) !== JSON.stringify(this.props.isVisible) && !this.props.isVisible
            && JSON.stringify(this.props.parametersArray) !== JSON.stringify(this.state.parametersArray)) {
            this.props.form.validateFields((err: any, values: any) => {
                if (err) {
                    this.props.context.notification(this.paramNotification,'Please, correct the mistakes', 'error')
                }
            });
        }
        if (JSON.stringify(prevProps.parametersArray) !== JSON.stringify(this.props.parametersArray)) {
            this.setState({parametersArray: this.props.parametersArray})
        }
        if (JSON.stringify(prevState.parametersArray) !== JSON.stringify(this.state.parametersArray)
            && this.props.isVisible
            && this.state.parametersArray?.length !== 0) {
            this.props.form.validateFields((err: any, values: any) => {
                if (!err) {
                    this.props.saveChanges!(this.state.parametersArray!, this.props.componentType);
                }
            });
        }

        if (this.state.parametersArray?.length === 0) {
            this.createNewRow()
        }
    }

    handleChange(e: any) {
        const target = JSON.parse(e);
        let parametersArray = this.state.parametersArray!.map( (f: any) => {
            if (f.index.toString() === target['index'].toString()) {
                const targetColumn = this.props.columnDefs!.find( (c: any) =>
                    c.get('field') === (f.datasetColumn || target['value'])
                );
                return {index: f.index,
                    datasetColumn: target['columnName'] === 'datasetColumn' ? target['value'] : f.datasetColumn,
                    operation: target['columnName'] === 'operation' ? target['value'] : f.operation,
                    value: target['columnName'] === 'value' ? target['value'] : f.value,
                    enable: target['columnName'] === 'enable' ? target['value'] : f.enable,
                    type: f.type || (targetColumn ? targetColumn.get('type') : undefined)}
            } else {
                return f
            }
        });
        this.setState({parametersArray})
    };

    deleteRow = (e: any) => {
        this.props.form.resetFields();
        let newServerParam: IServerQueryParam[] = [];
        this.state.parametersArray?.forEach((element:IServerQueryParam, index:number) => {
            if (element.index != e.index) {
                newServerParam.push({
                    index: newServerParam.length + 1,
                    datasetColumn: element.datasetColumn,
                    operation: element.operation,
                    value: element.value,
                    enable: (element.enable !== null ? element.enable : false),
                    type: element.type,
                    highlightType: element.highlightType,
                    backgroundColor: element.backgroundColor,
                    color: element.color
                })}
        });
        this.setState({parametersArray: newServerParam})
    };

    handleSubmit = (e: any) => {
        e.preventDefault();
        this.refresh();
    };

    createNewRow = () => {
        let parametersArray: any = this.state.parametersArray;
        parametersArray.push(
            {index: parametersArray.length + 1,
                datasetColumn: undefined,
                operation: undefined,
                value: undefined,
                enable: undefined,
                type: undefined,
                highlightType: undefined,
                backgroundColor: undefined,
                color: undefined});
        this.setState({parametersArray})
    };

    reset = () => {
        this.props.onChangeParameters!(undefined, this.props.componentType);
        this.setState({parametersArray:[]});
    };

    refresh = () => {
        this.props.form.validateFields((err: any, values: any) => {
            if (!err) {
                this.props.onChangeParameters!(this.state.parametersArray!, this.props.componentType)
            }
            else {
                this.props.context.notification('Sort notification','Please, correct the mistakes', 'error')
            }
        });
    };

    SortableItem = SortableElement(({value}: any) => {
        return <li className="SortableItem">
            <Row gutter={[8, 0]}>
                <Col span={24}>
                    {value.index}
                </Col>
            </Row>
        </li>
    });

    onSortEnd = ({oldIndex, newIndex}:any) => {
        let newState: IServerQueryParam[] = arrayMove(this.state.parametersArray!, oldIndex, newIndex)
        newState.forEach( (serverParam, index) => serverParam.index = index+1 );
        this.setState({parametersArray: newState});
    };

    SortableList = SortableContainer(({items}:any) => {
        return (
            <ul className="SortableList">
                {items.map((value:any) => (
                    <this.SortableItem key={`item-${value.index}`} index={value.index-1} value={value} />
                ))}
            </ul>
        );
    });

    render() {
        return (
            <Form style={{ marginTop: '30px' }} onSubmit={this.handleSubmit}>
                <Form.Item style={{marginTop: '-38px', marginBottom: '40px'}}>
                    <Col span={12}>
                        <div style={{display: "inherit", fontSize: '17px', fontWeight: 500, marginLeft: '18px', color: '#878787'}}>Сортировка</div>
                    </Col>
                    <Col span={12} style={{textAlign: "right"}}>
                        <Button
                            title="reset"
                            style={{width: '40px', marginRight: '10px'}}
                            key={'resetButton'}
                            value={'resetButton'}
                            onClick={this.reset}
                        >
                            <FontAwesomeIcon icon={faRedo} size='xs' color="#7b7979"/>
                        </Button>
                        <Button
                            title="add row"
                            style={{width: '40px', marginRight: '10px'}}
                            key={'createNewRowButton'}
                            value={'createNewRowButton'}
                            onClick={this.createNewRow}
                        >
                            <FontAwesomeIcon icon={faPlus} size='xs' color="#7b7979"/>
                        </Button>
                        <Button
                            title="run query"
                            style={{width: '40px'}}
                            key={'runQueryButton'}
                            value={'runQueryButton'}
                            htmlType="submit"
                        >
                            <FontAwesomeIcon icon={faPlay} size='xs' color="#7b7979"/>
                        </Button>
                    </Col>
                </Form.Item>
                <Form.Item>
                    {
                        <this.SortableList items={this.state.parametersArray!
                            .map((serverParam: any) => (
                                {
                                    ...serverParam,
                                    idDatasetColumn : `${JSON.stringify({index: serverParam.index, columnName: 'datasetColumn', value: serverParam.datasetColumn})}`,
                                    idOperation : `${JSON.stringify({index: serverParam.index, columnName: 'operation', value: serverParam.operation})}`,
                                }))} distance={3} onSortEnd={this.onSortEnd} helperClass="SortableHelper"/>
                    }
                </Form.Item>
            </Form>
        )
    }
}

export default withTranslation()(Form.create<Props & FormComponentProps & WithTranslation>()(DrawerParameterComponent))