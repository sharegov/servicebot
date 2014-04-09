/*
 * strophe.js
 *
 * Copyright (c) 2011 Miami-Dade County. All rights reserved.
 */
/*jshint indent: 4, white: false */
/*global $build: false, $iq: false, $msg: false, $pres: false, define: false, Strophe: false */

// Wraps StropheJS into an AMD module.
define('strophe', ['jquery', './vendor/strophe'], function ($) {
    'use strict';

    return $.extend({}, Strophe, {
        $build: $build,
        $iq: $iq,
        $msg: $msg,
        $pres: $pres
    });
});
