// dependency: ./namespace.js
// dependency: ext/package.js
// dependency: ./Form.js
// dependency: ./FormLayout.js

ZUTUBI.form.FormPanel = function(config)
{
    config.layout = new ZUTUBI.form.FormLayout({});
    ZUTUBI.form.FormPanel.superclass.constructor.call(this, config);
};

Ext.extend(ZUTUBI.form.FormPanel, Ext.form.FormPanel, {
    displayMode: false,
    buttonAlign: 'center',

    createForm: function()
    {
        delete this.initialConfig.listeners;
        return new ZUTUBI.form.Form(this.initialConfig);
    },

    onRender: function(ct, position)
    {
        ZUTUBI.form.FormPanel.superclass.onRender.call(this, ct, position);
        this.form.el.update('<table><tbody></tbody></table>');
        this.layoutTarget = this.form.el.first().first();
    },

    getLayoutTarget: function()
    {
        return this.layoutTarget;
    },

    add: function()
    {
        var a = arguments;
        for(var i = 0, len = a.length; i < len; i++)
        {
            a[i].form = this;
        }

        ZUTUBI.form.FormPanel.superclass.add.apply(this, a);
        return this;
    },

    markRequired: function(id, tooltip)
    {
        var cellEl = Ext.get('x-form-label-annotation-' + id);
        var spanEl = cellEl.createChild({tag: 'span', cls: 'required', id: id + '.required', html: '*'});
        if(tooltip)
        {
            spanEl.dom.qtip = tooltip;
        }
    },

    enableField: function(id)
    {
        var field = this.findById(id);
        if(field)
        {
            field.enable();

            var rowEl = this.getFieldRowEl(id);
            if (rowEl)
            {
                Ext.get(rowEl).removeClass('x-item-disabled');

                var actionDomEls = this.getFieldActionDomEls(id);
                if (actionDomEls)
                {
                    for(var i = 0; i < actionDomEls.length; i++)
                    {
                        Ext.get(actionDomEls[i]).removeClass('x-item-disabled');
                    }
                }
            }
        }
    },

    disableField: function(id)
    {
        var field = this.findById(id);
        if(field)
        {
            field.clearInvalid();
            field.disable();

            var rowEl = this.getFieldRowEl(id);
            if (rowEl)
            {
                Ext.get(rowEl).addClass('x-item-disabled');

                var actionDomEls = this.getFieldActionDomEls(id);
                if (actionDomEls)
                {
                    for(var i = 0; i < actionDomEls.length; i++)
                    {
                        Ext.get(actionDomEls[i]).addClass('x-item-disabled');
                    }
                }
            }
        }
    },

    getFieldActionDomEls: function(id)
    {
        var rowEl = this.getFieldRowEl(id);
        return Ext.query("a[class*='field-action']", rowEl.dom);
    },

    getFieldRowEl: function(id)
    {
        return Ext.get('x-form-row-' + id);
    },

    annotateField: function(id, annotationName, imageName, tooltip)
    {
        var rowEl = this.getFieldRowEl(id);
        var cellEl = rowEl.createChild({tag: 'td', cls: 'x-form-annotation'});
        var imageEl = cellEl.createChild({tag: 'img', src: imageName, id: id + '.' + annotationName});
        if(tooltip)
        {
            imageEl.dom.qtip = tooltip;
        }

        return imageEl;
    },

    updateButtons: function()
    {
        if(this.displayMode)
        {
            var dirty = this.form.isDirty();
            if(!dirty)
            {
                this.form.clearInvalid();
            }

            for(var i = 0; i < this.buttons.length; i++)
            {
                if(dirty)
                {
                    this.buttons[i].enable();
                }
                else
                {
                    this.buttons[i].disable();
                }
            }
        }
    },

    submitForm: function (value)
    {
        var f = this.getForm();
        Ext.get(this.formName + '.submitField').dom.value = value;
        if(value == 'cancel')
        {
            Ext.DomHelper.append(f.el.parent(), {tag: 'input', type: 'hidden', name: 'cancel', value: 'true'});
        }

        f.clearInvalid();
        if (this.ajax)
        {
            window.formSubmitting = true;
            f.submit({
                clientValidation: false,
                waitMsg: 'Submitting...'
            });
        }
        else
        {
            if(value == 'cancel' || f.isValid())
            {
                f.el.dom.submit();
            }
        }
    },

    defaultSubmit: function()
    {
        if (!this.readOnly)
        {
            this.submitForm(this.defaultSubmitValue);
        }
    },

    handleFieldKeypress: function (evt)
    {
        if (evt.getKey() != evt.RETURN || this.readOnly)
        {
            return true;
        }
        else
        {
            this.defaultSubmit();
            evt.preventDefault();
            return false;
        }
    },

    attachFieldKeyHandlers: function()
    {
        var panel = this;
        var form = this.getForm();
        form.items.each(function(field) {
            var el = field.getEl();
            if(el)
            {
                el.set({tabindex: window.nextTabindex++ });

                if (field.submitOnEnter)
                {
                    el.on('keypress', function(event){ return panel.handleFieldKeypress(event); });
                }
                el.on('keyup', panel.updateButtons.createDelegate(panel));
                el.on('click', panel.updateButtons.createDelegate(panel));
            }
        });
    }
});
