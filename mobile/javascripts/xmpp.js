/*
 * xmpp.js
 *
 * Copyright (c) 2011 Miami-Dade County. All rights reserved.
 */
define({
    // Connection to Jabber server.
    connection: null,
    // Indicates whether user has joined room.
    joined: false,
    // User's nickname.
    nickname: null,
    // Jabber namespaces used by chat.
    ns: {
        MUC: "http://jabber.org/protocol/muc"   // Multi-user chat.
    },
    // List of members that are presently participating in chat.
    participants: {},
    // Help room's JID.
    room: null
});