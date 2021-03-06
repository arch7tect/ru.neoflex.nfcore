package ru.neoflex.nfcore.dsl

import groovy.text.StreamingTemplateEngine
import groovy.text.Template

class TemplateBuilder {
    static test = '''import ru.neoflex.nfcore.dsl.TemplateBuilder

def builder = TemplateBuilder.build {
    mDefine "row", {
        """\\
        |{
        |  <% for (col in row) {%><%=col%>, <%}%>
        |}
        |"""
    }
    mDefine "main", {
        """\\
        |{
        |  <% for (row in data) {%><%= mCall "row", [row: row]%><%}%>
        |}
        |"""
    }
}

def model = [data: [
        ["a", "b", "c"],
        ["x", "y", "z"],
]]
builder.mCall("main", model)
println builder.out
'''
    def templateEngine = new StreamingTemplateEngine()
    Map<String, Template> templateMap = new HashMap<>()
    def out = new TemplateWriter()
    static class TemplateWriter extends StringWriter {
        String last = ""

        void superWrite(String s) {
            super.write(s);
        }

        @Override
        void write(String str) {
            super.write(str)
            last = (last + str).split('''\n''', -1).last()
        }
    }

    static TemplateBuilder build(@DelegatesTo(TemplateBuilder) Closure closure) {
        TemplateBuilder builder = new TemplateBuilder()
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = builder
        closure.call()
        return builder
    }

    void mDefine(String name, @DelegatesTo(TemplateBuilder) Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = this
        String body = closure.call()
        body = body.split('''\r?\n''', -1)
                .collect {it.replaceAll('''^[\t ]*[|]''', "")}
                .join('''\n''')
        Template template = templateEngine.createTemplate(body)
        templateMap.put(name, template)
    }

    String mCall(String name, Map binding) {
        def prefix = out.last.replaceAll('''[^\t ]''', " ")
        def template = templateMap.get(name)
        Closure closure = template.make(binding)
        closure.mCall = {String x, Map y->mCall(x, y)}
//        template.resolveStrategy = Closure.TO_SELF
        def temp = new TemplateWriter()
        closure.call(temp)
        def text = temp.toString()
        text = text.split('''\n''', -1).join('''\n''' + prefix)
        return text

    }
}
