define("strophe", ["strophe/strophe"], function () {
    return {
        Strophe: Strophe,
        "$build": $build,
        "$iq": $iq,
        "$msg": $msg,
        "$pres": $pres
    };
});