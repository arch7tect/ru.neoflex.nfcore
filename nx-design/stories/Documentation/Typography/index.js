import React, { Component, Fragment } from 'react';
import {NXCol, NXRow} from "../../../index";

export default class TypographyPageDoc extends Component {
    state = {}
    render() {
        return (
            <Fragment>
                    <h1 className="title">Типографика и текст в интерфейсах</h1>
                <section className="example">


                    <p className="text">
                        Шрифт -  Roboto
                    </p>
                    <NXRow>
                        <NXCol span={8}><h1>56px</h1></NXCol>
                        <NXCol span={8}><h1>H1</h1></NXCol>
                        <NXCol span={8}><h1>Text</h1></NXCol>
                    </NXRow>
                    <NXRow>
                        <NXCol span={8}><h2 className={'medium'}>32px</h2></NXCol>
                        <NXCol span={8}><h2 className={'medium'}>H2/medium</h2></NXCol>
                        <NXCol span={8}><h2 className={'medium'}>Text</h2></NXCol>
                    </NXRow>
                    <NXRow>
                        <NXCol span={8}><h2>32px</h2></NXCol>
                        <NXCol span={8}><h2>H2/regular</h2></NXCol>
                        <NXCol span={8}><h2>Text</h2></NXCol>
                    </NXRow>
                    <NXRow>
                        <NXCol span={8}><h3 className={'medium'}>20px</h3></NXCol>
                        <NXCol span={8}><h3 className={'medium'}>H3/medium</h3></NXCol>
                        <NXCol span={8}><h3 className={'medium'}>Text</h3></NXCol>
                    </NXRow>
                    <NXRow>
                        <NXCol span={8}><h3>20px</h3></NXCol>
                        <NXCol span={8}><h3>H3/regular</h3></NXCol>
                        <NXCol span={8}><h3>Text</h3></NXCol>
                    </NXRow>
                    <NXRow>
                        <NXCol span={8}><h4 className={'medium'}>16px</h4></NXCol>
                        <NXCol span={8}><h4 className={'medium'}>H4/medium</h4></NXCol>
                        <NXCol span={8}><h4 className={'medium'}>Text</h4></NXCol>
                    </NXRow>
                    <NXRow>
                        <NXCol span={8}><h4>16px</h4></NXCol>
                        <NXCol span={8}><h4>H4/regular</h4></NXCol>
                        <NXCol span={8}><h4>Text</h4></NXCol>
                    </NXRow>
                    <NXRow>
                        <NXCol span={8}><h4 className={'light'}>16px</h4></NXCol>
                        <NXCol span={8}><h4 className={'light'}>H4/light</h4></NXCol>
                        <NXCol span={8}><h4 className={'light'}>Text</h4></NXCol>
                    </NXRow>
                    <NXRow>
                        <NXCol span={8}>14px</NXCol>
                        <NXCol span={8}>body/medium</NXCol>
                        <NXCol span={8}>Text</NXCol>
                    </NXRow>
                    <NXRow>
                        <NXCol span={8}>14px</NXCol>
                        <NXCol span={8}>body/regular</NXCol>
                        <NXCol span={8}>Text</NXCol>
                    </NXRow>
                    <NXRow>
                        <NXCol span={8}>14px</NXCol>
                        <NXCol span={8}>body/light</NXCol>
                        <NXCol span={8}>Text</NXCol>
                    </NXRow>
                </section>
            </Fragment>
        );
    }
}

