/*
 * events.js
 *
 * Copyright (c) 2011 Miami-Dade County. All rights reserved.
 */
define({
    message: {
        ADD: "servicebot_message_add"
    },
    xmpp: {
        jid: {
            GET:      "servicebot_xmpp_jid_get",
            RECEIVED: "servicebot_xmpp_jid_received"
        },
        on: {
            GROUPCHAT: "servicebot_xmpp_on_message_groupchat",
            PRESENCE:  "servicebot_xmpp_on_presence"
        },
        CONNECT:      "servicebot_xmpp_connect",
        CONNECTED:    "servicebot_xmpp_connected",
        DISCONNECT:   "servicebot_xmpp_disconnect",
        DISCONNECTED: "servicebot_xmpp_disconnected",
        JOINED:       "servicebot_xmpp_joined"
    }
});
 