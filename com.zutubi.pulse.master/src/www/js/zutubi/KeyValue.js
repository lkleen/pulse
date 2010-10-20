// dependency: ./namespace.js
// dependency: ext/package.js

/**
 * A utility class used by UI components to help render keyed-values from data objects.  Initialised
 * from a config object as outlined below.  If a string is provided as the config, it is used as the
 * name and other fields are given default values.
 *
 * @cfg {String} name     The name of the field, used to look up the value in data objects.
 * @cfg {String} key      Key text to use as a label in the UI (defaults to name). 
 * @cfg {String} renderer Function to turn the raw string value into HTML to use in the UI (defaults
 *                        to a simple HTML encode)
 */
Zutubi.KeyValue = function(config) {
    if (typeof config == 'string')
    {
        config = {name: config};
    }

    Ext.apply(this, config, {
        renderer: Ext.util.Format.htmlEncode
    });
    
    if (!this.key)
    {
        this.key = this.name;
    }
};

Zutubi.KeyValue.prototype = {
    getRawValue: function(data) {
        return data[this.name];
    },

    getRenderedValue: function(data) {
        return this.renderer(data[this.name]);
    }
};
