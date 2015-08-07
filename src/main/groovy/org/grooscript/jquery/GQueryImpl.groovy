package org.grooscript.jquery

import org.grooscript.asts.GsNative
import org.grooscript.rx.Observable

/**
 * Created by jorge on 15/02/14.
 */
class GQueryImpl implements GQuery {

    GQueryList bind(String selector, target, String nameProperty, Closure closure = null) {
        GQueryList.of(selector).bind(target, nameProperty, closure)
    }

    boolean existsId(String id) {
        GQueryList.of("#${id}").hasResults()
    }

    boolean existsName(String name) {
        GQueryList.of("[name='${name}']").hasResults()
    }

    boolean existsGroup(String name) {
        GQueryList.of("input:radio[name='${name}']").hasResults()
    }

    GQueryList onEvent(String selector, String nameEvent, Closure func) {
        GQueryList.of(selector).onEvent(nameEvent, func)
    }

    @GsNative
    void doRemoteCall(String url, String type, params, Closure onSuccess, Closure onFailure, objectResult = null) {/*
        $.ajax({
            type: type, //GET or POST
            data: gs.toJavascript(params),
            url: url,
            dataType: 'text'
        }).done(function(newData) {
            if (onSuccess) {
                onSuccess(gs.toGroovy(jQuery.parseJSON(newData), objectResult));
            }
        })
        .fail(function(error) {
            if (onFailure) {
                onFailure(error);
            }
        });
    */}

    @GsNative
    void onReady(Closure func) {/*
        $(document).ready(func);
    */}

    void attachMethodsToDomEvents(obj) {
        obj.metaClass.methods.each { method ->
            if (method.name.endsWith('Click')) {
                def shortName = method.name.substring(0, method.name.length() - 5)
                if (existsId(shortName)) {
                    onEvent('#'+shortName, 'click', obj.&"${method.name}")
                }
            }
            if (method.name.endsWith('Submit')) {
                def shortName = method.name.substring(0, method.name.length() - 6)
                if (existsId(shortName)) {
                    onEvent('#'+shortName, 'submit', obj.&"${method.name}" << { it.preventDefault() })
                }
            }
            if (method.name.endsWith('Change')) {
                def shortName = method.name.substring(0, method.name.length() - 6)
                if (existsId(shortName)) {
                    onChange('#'+shortName, obj.&"${method.name}")
                }
            }
        }
    }

    GQueryList onChange(String selector, Closure closure) {
        GQueryList.of(selector).onChange closure
    }

    GQueryList focusEnd(String selector) {
        GQueryList.of(selector).focusEnd()
    }

    void bindAllProperties(target) {
        target.properties.each { name, value ->
            if (existsId(name)) {
                bind("#$name", target, name)
            }
            if (existsName(name)) {
                bind("[name='$name']", target, name)
            }
            if (existsGroup(name)) {
                bind("input:radio[name='${name}']", target, name)
            }
        }
    }

    void bindAll(target) {
        bindAllProperties(target)
        attachMethodsToDomEvents(target)
    }

    Observable observeEvent(String selector, String nameEvent, Map data = [:]) {
        def observable = Observable.listen()
        call(selector).on(nameEvent, data, { event ->
            observable.produce(event)
        })
        observable
    }

    GQueryList call(String selector) {
        GQueryList.of(selector)
    }
}

class GQueryList {

    def list
    String selec

    static GQueryList of(String selector) {
        new GQueryList(selector)
    }

    GQueryList(String selector) {
        selec = selector
        list = jqueryList(selector)
    }

    @GsNative
    def methodMissing(String name, args) {/*
        return gSobject.list[name].apply(gSobject.list, args);
    */}

    @GsNative
    GQueryList withResultList(Closure cl) {/*
        if (gSobject.list.length) {
            cl(gSobject.list.toArray());
        }
        return gSobject;
    */}

    @GsNative
    boolean hasResults() {/*
        return gSobject.list.length > 0;
    */}

    @GsNative
    GQueryList onEvent(String nameEvent, Closure cl) {/*
        gSobject.list.on(nameEvent, cl);
        return gSobject;
    */}

    @GsNative
    GQueryList onChange(Closure cl) {/*
        var jq = gSobject.list;

        if (jq.is(":text")) {
            jq.bind('input', function() {
                cl($(this).val());
            });
        } else if (jq.is('textarea')) {
            jq.bind('input propertychange', function() {
                cl($(this).val());
            });
        } else if (jq.is(":checkbox")) {
            jq.change(function() {
                cl($(this).is(':checked'));
            });
        } else if (jq.is(":radio")) {
            jq.change(function() {
                cl($(this).val());
            });
        } else if (jq.is("select")) {
            jq.bind('change', function() {
                cl($(this).val());
            });
        } else {
            console.log('Not supporting onChange for selector: ' + gSobject.selec);
        }
        return gSobject;
    */}

    @GsNative
    GQueryList focusEnd() {/*
        var jq = gSobject.list;

        if (jq.length) {
            if (jq.is(":text") || jq.is('textarea')) {
                var originalValue = jq.val();
                jq.val('');
                jq.blur().focus().val(originalValue);
            } else {
                jq.focus();
            }
        }
        return gSobject;
    */}

    @GsNative
    GQueryList bind(target, String nameProperty, Closure closure = null) { /*
        var jq = gSobject.list;
        //Create set method
        var nameSetMethod = 'set'+nameProperty.capitalize();

        if (jq.is(":text")) {
            target[nameSetMethod] = function(newValue) {
                this[nameProperty] = newValue;
                jq.val(newValue);
                if (closure) { closure(newValue); };
            };
            jq.bind('input', function() {
                var currentVal = $(this).val();
                target[nameProperty] = currentVal;
                if (closure) { closure(currentVal); };
            });
        } else if (jq.is('textarea')) {
            target[nameSetMethod] = function(newValue) {
                this[nameProperty] = newValue;
                jq.val(newValue);
                if (closure) { closure(newValue); };
            };
            jq.bind('input propertychange', function() {
                var currentVal = $(this).val();
                target[nameProperty] = currentVal;
                if (closure) { closure(currentVal); };
            });
        } else if (jq.is(":checkbox")) {
            target[nameSetMethod] = function(newValue) {
                this[nameProperty] = newValue;
                jq.prop('checked', newValue);
                if (closure) { closure(newValue); };
            };
            jq.change(function() {
                var currentVal = $(this).is(':checked');
                target[nameProperty] = currentVal;
                if (closure) { closure(currentVal); };
            });
        } else if (jq.is(":radio")) {
            target[nameSetMethod] = function(newValue) {
                this[nameProperty] = newValue;
                $(gSobject.selec +'[value="' + newValue + '"]').prop('checked', true);
                if (closure) { closure(newValue); };
            };
            jq.change(function() {
                var currentVal = $(this).val();
                target[nameProperty] = currentVal;
                if (closure) { closure(currentVal); };
            });
        } else if (jq.is("select")) {
            target[nameSetMethod] = function(newValue) {
                this[nameProperty] = newValue;
                jq.val(newValue);
                if (closure) { closure(newValue); };
            };
            jq.bind('change', function() {
                var currentVal = $(this).val();
                target[nameProperty] = currentVal;
                if (closure) { closure(currentVal); };
            });
        } else {
            console.log('Not supporting bind for selector ' + gSobject.selec);
        }
        return gSobject;
    */}

    @GsNative
    private jqueryList(String selec) {/*
        return $(selec);
    */}
}
