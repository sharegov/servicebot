/*
 * platform.js
 *
 * Copyright (c) 2011 Miami-Dade County. All rights reserved.
 */
define(function () {
    var agent = navigator.userAgent.toLowerCase(),
        mobile = agent.indexOf("iphone") !== -1 || agent.indexOf("ipod") !== -1 || agent.indexOf("ipad") !== -1 && agent.indexOf("simulator") === -1;
    
    return {
        click: mobile ? "tap" : "click",
        desktop: !mobile,
        mobile: mobile
    };
});
