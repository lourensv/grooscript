/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grooscript.jquery

import org.grooscript.FunctionalTest
import org.grooscript.GrooScript

class TestFunctionalBinder extends FunctionalTest {

    String htmlResponse() {
        def result = '<html><head><title>Title</title></head><body>'
        result += script(jsFileText('grooscript.js'))
        result += script(jsFileText('jquery.min.js'))
        result += script(jsFileText('grooscript-tools.js'))
        result += script(GrooScript.convert(bookClass))
        result += script('var book = Book(); var gQuery = GQueryImpl();')
        result += script('$(document).ready(function() { gQuery.bindAll(book); book.init()});')
        result += '<p>Author:<input type="text" id="author"></p>'
        result += '<p>Title:<input type="text" name="title"></p>'
        result += '<p><input type="checkbox" id="hasEbook">Has Ebook</p>'
        result += '<p><input type="radio" name="numberPages" id="smallSize" value="small">Small' +
                '<input type="radio" name="numberPages" id="bigSize" value="big">Big</p>'
        result += '<p>Country:<select id="country"><option/><option value="spain">Spain</option>' +
                '<option value="eeuu">EE.UU.</option></select></p>'
        result += '<p><input type="button" id="do" value="Click!"/></p>'
        result += '</body></html>'
        result
    }

    void testBindPropertiesAndClick() {
        assertScript """
    @org.grooscript.asts.PhantomJsTest(url = '${FunctionalTest.HTML_ADDRESS}', waitSeconds = 1)
    void doTest() {
        assert \$('#author').val() == 'Jorge', "Value is: \${\$('#author').val()}"
        assert \$("input[name='title']").val() == 'Grooscript', "Value is: \${\$("[name='title']").val()}"
        assert \$("#hasEbook").is(':checked'), "hasEbook not checked"
        assert \$('#bigSize').is(':checked'), "radio big isn't checked"
        assert \$('#country').val() == 'spain', "country select isn't spain"
        assert book.numberChangesCountry == 1, "titleChange not processed (\${book.numberChangesCountry})"
        book.country = 'eeuu'
        assert \$('#country').val() == 'eeuu', "country select isn't eeuu"
        \$('#do').click()
        assert book.numberOfClicks == 1, "Incorrect number of clicks: \${book.numberOfClicks}"
    }
    doTest()
"""
    }

    private getBookClass() {
        '''
        class Book {
            String author
            String title
            boolean hasEbook = false
            String numberPages
            String country
            def numberOfClicks = 0
            def numberChangesCountry = 0

            def init() {
                setAuthor("Jorge")
                setTitle("Grooscript")
                setHasEbook(true)
                setNumberPages('big')
                setCountry('spain')
                $('#country').trigger("change")
            }

            def doClick() {
                numberOfClicks++
            }

            def countryChange() {
                numberChangesCountry++
            }
        }
        '''
    }
}