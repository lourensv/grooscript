package org.grooscript.asts

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grooscript.GrooScript
import org.grooscript.GsConverter

import java.lang.reflect.Modifier

/**
 * User: jorgefrancoleza
 * Date: 30/03/13
 */
@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
public class PhantomJsTestImpl implements ASTTransformation {

    def phantomJsText = '''
var page = require('webpage').create();

page.onConsoleMessage = function(msg) {
    console.log('CONSOLE: ' + msg);
};

page.open('{{URL}}', function (status) {
    if (status !== 'success') {
        console.log('Fail loading url.');
        phantom.exit(1);
    } else {
        page.libraryPath = '{{LIBRARY_PATH}}'
        if (page.injectJs('kimbo.min.js') && page.injectJs('grooscript.js')) {
            //console.log('Evaluating...');
            var result = page.evaluate(function() {

                var gSresult = { number:0 , tests: [], console: ''};
                function gSassert(value, text) {

                    var test = { result: value, text: value.toString()}
                    if (arguments.length == 2 && arguments[1]!=null && arguments[1]!=undefined) {
                        test.text = text;
                    }
                    gSresult.tests[gSresult.number++] = test;
                };

                function gSprintln(value) {
                    if (gSconsole != "") {
                        gSconsole = gSconsole + "\\n";
                    }
                    gSconsole = gSconsole + 'Console: ' + value
                };

                function grooKimbo(selector,other) {
                    return gSlist(window.Kimbo(selector,other));
                }
                window.$ = grooKimbo;

                //Begin grooscript code
                {{GROOSCRIPT}}
                //End  grooscript code

                gSresult.console = gSconsole;

                return gSresult;
            });
            console.log('Number of tests: '+result.number);
            if (result.number > 0) {
                var i;
                for (i=0;i<result.tests.length;i++) {
                    console.log('Test ('+i+') Result:'+(result.tests[i].result==true?'OK':'FAIL')+
                        ' Desc:'+result.tests[i].text);
                }
            }
            console.log(result.console);
            phantom.exit()
        } else {
            console.log('Fail in inject.');
            phantom.exit(1);
        }
    }
});'''

    public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
        //Start
        if (!nodes[0] instanceof AnnotationNode ||
            !nodes[1] instanceof MethodNode) {
            return
        }

        MethodNode method = (MethodNode) nodes[1]
        Statement testCode = method.getCode()

        def messageError
        AnnotationNode node = (AnnotationNode) nodes[0]
        String url = node.getMember('url').text

        if (!url) {
            messageError = 'url not defined, use @PhantomJsTest(url=\'http://grooscript.org\')'
        } else {
            method.getDeclaringClass().addProperty('phantomjsUrl', Modifier.STATIC, ClassHelper.STRING_TYPE,
                    new ConstantExpression(url),null,null)
        }
        if (!messageError) {
            GsConverter converter = new GsConverter()

            def jsTest
            try {
                jsTest = converter.processAstListToJs([testCode])
            } catch (e) {
                messageError = 'Error converting code ->'+e.message
            }

            if (!messageError) {
                String text = phantomJsText
                text = text.replace('{{GROOSCRIPT}}',jsTest)

                //Save in a static variable content of the file
                method.getDeclaringClass().addProperty('phantomjsScript', Modifier.STATIC, ClassHelper.STRING_TYPE,
                        new ConstantExpression(text),null,null)
            }
        }
        method.getDeclaringClass().addProperty('phantomjsError', Modifier.STATIC, ClassHelper.STRING_TYPE,
                new ConstantExpression(messageError),null,null)

        method.setCode new AstBuilder().buildFromCode {
            if (phantomjsError) {
                println 'PhantomJs Error: '+phantomjsError; return
            }
            if (!System.getProperty('JS_LIBRARIES_PATH')) {
                println 'Need define property JS_LIBRARIES_PATH, folder with grooscript.js and kimbo.min.js'; return
            }
            if (!System.getProperty('PHANTOMJS_HOME')) {
                println 'Need define property PHANTOMJS_HOME, PhantomJs folder'; return
            }
            println "Starting PhantomJs test in ${phantomjsUrl}..."
            def nameFile = 'phantomjs.js'
            try {
                //Save the file
                phantomjsScript = phantomjsScript.replace('{{LIBRARY_PATH}}', System.getProperty('JS_LIBRARIES_PATH'))
                phantomjsScript = phantomjsScript.replace('{{URL}}', phantomjsUrl)
                new File(nameFile).text = phantomjsScript

                //Execute PhantomJs
                String command = "${System.getProperty('PHANTOMJS_HOME')}/bin/phantomjs ${nameFile}"
                def proc = command.execute()
                proc.waitFor()
                def exit = proc.in.text
                if (!exit) {
                    println 'Error executing command:'+command
                    println "return code: ${proc.exitValue()}"
                    println "stderr: ${proc.err.text}"
                    assert false, 'Error launching PhantomJs'
                } else {
                    //println "stdout: ${exit}"
                    if (exit.startsWith('Number of tests:')) {
                        exit.eachLine { String line->
                            //println 'Line.>'+line
                            if (line) {
                                if (line.contains('Result:FAIL')) {
                                    assert false,line.substring(line.indexOf(' Desc:')+6)
                                } else if (line.startsWith('Console:')) {
                                    println line.substring(8)
                                } else {
                                    assert true,line.substring(line.indexOf(' Desc:')+6)
                                }
                            }
                        }
                    } else {
                        println 'Error executing tests.\n'+proc.in.text
                        assert false, 'Error executing PhantomJs'
                    }
                }
                //println 'End PhantomJs test.'
            } catch (e) {
                println 'Error PhantomJs Test ->'+e.message
                if (e.message && e.message.startsWith('Cannot run program')) {
                    assert false,'Don\'t find PhantomJs: '+e.message
                } else {
                    assert false,'Error executing PhantomJs: '+e.message
                }
            } finally {
                File file = new File(nameFile)
                if (file && file.exists()) {
                    file.delete()
                }
            }
            println 'PhantomJs test finished.'
            return null
        }
    }
}