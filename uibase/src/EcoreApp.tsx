import * as React from "react";
import {Button, Icon, Layout, Menu, notification} from 'antd';
import 'antd/dist/antd.css';
import './styles/EcoreApp.css';
import {API, Error, IErrorHandler} from './modules/api'
import MetaBrowserTrans from "./components/MetaBrowser";
import {ResourceEditor} from "./components/ResourceEditor"
import {Link, Redirect, Route, RouteComponentProps, Switch} from "react-router-dom";
import QueryRunnerTrans from "./components/QueryRunner";
import Login from "./components/Login";
import {DataBrowser} from "./components/DataBrowser";
import {MainApp} from "./MainApp";
import {withTranslation, WithTranslation} from "react-i18next";
import Ecore from "ecore";
import DynamicComponent from "./components/DynamicComponent"
import MandatoryReportingTrans from "./components/app/MandatoryReporting";

const { Header, Content, Sider } = Layout;
const ResourceEditorTrans = withTranslation()(ResourceEditor);

export interface Props extends RouteComponentProps {
    name: string;
}

interface State {
    principal?: any;
    appName: string;
    languages: string[];
    notifierDuration: number;
}

class EcoreApp extends React.Component<any, State> {

    constructor(props: any) {
        super(props);
        this.state = {
            principal: undefined,
            appName: props.appName,
            languages: [],
            notifierDuration: 0
        };
    }

    onRightMenu(e : any) {
        if (e.key === "logout") {
            API.instance().logout().then(() => {
                this.setState({principal : undefined});
            });
            this.props.history.push('')
        }
        else if (e.key === "developer") {
            this.props.history.push('/settings/data');
        }
        else if (e.key === "app") {
            this.props.history.push('/app');
        }
        else if (e.key === "testComponent") {
            this.props.history.push('/test');
        }
        else if (e.key === "showNotifications"){
            this.setState({notifierDuration : 0});
            localStorage.setItem('notifierDuration', '0');
        }
        else if (e.key === "autoHideNotifications"){
            this.setState({notifierDuration : 3});
            localStorage.setItem('notifierDuration', '3');
        }
        else if (e.key === "mandatoryreporting"){
            this.props.history.push('/mandatoryreporting');
        }
    }

    setPrincipal = (principal: any)=>{
        this.setState({principal}, API.instance().init)
    };

    getLanguages() {
        const prepared: Array<string> = [];
        API.instance().fetchAllClasses(false).then(classes => {
            const temp = classes.find((c: Ecore.EObject) => c._id === "//Lang");
            if (temp !== undefined) {
                API.instance().findByClass(temp, {contents: {eClass: temp.eURI()}})
                    .then((resources) => {
                        resources.map((r) =>
                                prepared.push(r.eContents()[0].get('name'))
                        );
                        this.setState({languages: prepared.sort()})
                    })
            }
        })
    }

    renderDev = () => {
        let principal = this.state.principal as any;
        const {t, i18n} = this.props as Props & WithTranslation;
        const setLang = (lng: any) => {
            i18n.changeLanguage(lng)
        };
        return (
            <Layout style={{height: '100vh'}}>
                <Header style={{height: '40px', padding: "0px"}}>
                    <Menu theme="dark" mode="horizontal" onClick={(e) => this.onRightMenu(e)} style={{float: "right", height: '100%'}}>
                        <Menu.SubMenu title={<span><Icon type="user" style={{fontSize: '17px', marginRight: '0'}}/> {principal.name}</span>} style={{float: "right", height: '100%', top: '-3px'}}>
                            <Menu.Item key={'logout'}><Icon type="logout" style={{fontSize: '17px'}}/>{t('logout')}</Menu.Item>
                            <Menu.Item key={'developer'}><Icon type="setting" style={{fontSize: '17px'}} theme="filled"/>{t('developer')}</Menu.Item>
                            <Menu.Item key={'app'}><Icon type="sketch" style={{fontSize: '17px'}}/>App</Menu.Item>
                            <Menu.Item key={'testComponent'}><Icon type="coffee" style={{fontSize: '17px'}}/>Test component</Menu.Item>
                            <Menu.SubMenu  title={<span><Icon type="global" style={{fontSize: '17px'}}/>{t('language')}</span>}>
                                {
                                    this.state.languages.map((c: any) =>
                                        <Menu.Item key={c} onClick={() =>
                                            setLang(c)
                                        }>
                                            <Icon type="flag" style={{fontSize: '17px'}}/>
                                            {c.toUpperCase()}
                                        </Menu.Item>)
                                }
                            </Menu.SubMenu>
                            <Menu.SubMenu  title={<span><Icon type="notification" style={{fontSize: '17px'}}/>Notification</span>}>
                                <Menu.Item key={'showNotifications'}><Icon type="eye" style={{fontSize: '17px'}} />Show constantly</Menu.Item>
                                <Menu.Item key={'autoHideNotifications'}><Icon type="clock-circle" style={{fontSize: '17px'}} />Autohide</Menu.Item>
                            </Menu.SubMenu>
                            <Menu.Item key={'mandatoryreporting'}><Icon type="calendar" style={{fontSize: '17px'}}/>Mandatory Reporting</Menu.Item>
                        </Menu.SubMenu>
                    </Menu>
                </Header>

                <Switch>
                    <Redirect from={'/'} exact={true} to={'/app'}/>
                    <Route path='/app' component={this.renderStartPage}/>
                    <Route path='/settings' component={this.renderSettings}/>
                    <Route path='/test' component={this.renderTest}/>
                    <Route path='/mandatoryreporting' component={this.renderMandatoryReporting}/>
                </Switch>
            </Layout>
        )
    }

    renderTest = ()=> {
        return (
            <div>
                {/*Correct test example*/}
                <DynamicComponent componentPath={"components/reports/component.js"} componentName={"Report"}/>
                {/*Example with error*/}
                <DynamicComponent componentPath={"components/reports/component.js"} componentName={"UnCorrect"}/>
            </div>
        )}

    renderMandatoryReporting = ()=> {
        return (
            <div>
                <MandatoryReportingTrans/>
            </div>
        )};

    renderSettings=()=>{
        const {t} = this.props as Props & WithTranslation;
        let selectedKeys = ['metadata', 'data', 'query']
            .filter(k => this.props.location.pathname.split('/').includes(k));
        return (
            <Layout>
                <Sider collapsible breakpoint="lg" collapsedWidth="0">
                    <Menu className="dark" theme="dark" mode="inline" selectedKeys={selectedKeys}>
                        <Menu.Item style={{ fontSize: 14 }} key={'metadata'}><Link to={`/settings/metadata`}>{t('metadata')}</Link></Menu.Item>
                        <Menu.Item style={{ fontSize: 14 }} key={'data'}><Link to={`/settings/data`}>{t('data')}</Link></Menu.Item>
                        <Menu.Item style={{ fontSize: 14 }} key={'query'}><Link to={`/settings/query`}>{t('query')}</Link></Menu.Item>
                    </Menu>
                </Sider>
                <Layout>
                    <Content>
                        <Switch>
                            <Route path='/settings/metadata' component={MetaBrowserTrans}/>
                            <Route exact={true} path='/settings/data' component={DataBrowser}/>
                            <Route path='/settings/data/:id/:ref' component={ResourceEditorTrans}/>
                            <Route path='/settings/query' component={QueryRunnerTrans}/>
                        </Switch>
                    </Content>
                </Layout>
            </Layout>
        )
    }

    renderStartPage = ()=>{
        return (
            <MainApp {...this.props}/>
        )
    }

    componentDidMount(): void {
        if (!this.state.languages.length) {this.getLanguages()}
        const _this = this;
        let errorHandler : IErrorHandler = {
            handleError(error: Error): void {
                if (error.status === 401) {
                    _this.setState({principal: undefined});
                }
                let btn = (<Button type="link" size="small" onClick={() => notification.destroy()}>
                    Close All
                </Button>);
                let key = error.error + error.status + error.message;
                    notification.error({
                        message: "Error: " + error.status + " (" + error.error + ")",
                        btn,
                        duration: _this.state.notifierDuration,
                        description: error.message,
                        key,
                        style: {
                            width: 400,
                            marginLeft: -10,
                            marginTop: 16,
                            wordWrap: "break-word"
                        },
                    })
            }
        } as IErrorHandler;
        API.instance().addErrorHandler(errorHandler);

        const localDuration = localStorage.getItem('notifierDuration');
        localDuration && this.setState({notifierDuration: Number(localDuration) });
    }

    render = () => {
        return (
                <Layout>
                    {this.state.principal === undefined ?
                        <Layout>
                            <Login onLoginSucceed={this.setPrincipal}/>
                        </Layout>
                        :
                        <Layout>
                            {this.renderDev()}
                        </Layout>
                    }
                </Layout>
        )
    }
}
const EcoreAppTrans = withTranslation()(EcoreApp);
export default EcoreAppTrans;
