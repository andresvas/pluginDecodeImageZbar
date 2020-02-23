var argscheck = require('cordova/argscheck'),
    exec      = require('cordova/exec');

function ZBar () {};

ZBar.prototype = {

    scan: function (params, success, failure)
    {
        argscheck.checkArgs('*fF', 'CsZBarTodo1.scan', arguments);

        params = params || {};
        if(params.text_title === undefined) params.text_title = "Scan QR Code";
        if(params.text_instructions === undefined) params.text_instructions = "Please point your camera at the QR code.";
        if(params.camera != "front") params.camera = "back";
        if(params.flash != "on" && params.flash != "off") params.flash = "auto";

        exec(success, failure, 'CsZBarTodo1', 'scan', [params]);
    },

    gallery: function (params, success, failure)
    {
        argscheck.checkArgs('*fF', 'CsZBarTodo1.gallery', arguments);

        params = params || {};
        if(params.text_title === undefined) params.text_title = "Scan QR Code";
        if(params.text_instructions === undefined) params.text_instructions = "Please point your camera at the QR code.";
        if(params.camera != "front") params.camera = "back";
        if(params.flash != "on" && params.flash != "off") params.flash = "auto";

        exec(success, failure, 'CsZBarTodo1', 'gallery', [params]);
    },

};

module.exports = new ZBar;
