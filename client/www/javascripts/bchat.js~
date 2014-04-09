/*
 * chat.js
 *
 * Copyright (c) 2011 Miami-Dade County. All rights reserved.
 */
/*jshint indent: 4, white: false */
/*global define: false */

define(['jquery', 'strophe', './jquery/plugins/corner', './jquery/plugins/layout', './jqueryui/dialog'], function ($, Strophe) {
    'use strict';

    window.chat = window.chat || {};
    var chat = window.chat;

    // Bounded events.
    chat.events = {
        ADD_MESSAGE: "chat_add_message",
        xmpp: {
            CONNECT: "chat_xmpp_connect",
            CONNECTED: "chat_xmpp_connected",
            DISCONNECT: "chat_xmpp_disconnect",
            DISCONNECTED: "chat_xmpp_disconnected",
            JOINED: "chat_xmpp_joined",
            on: {
                GROUPCHAT: "chat_xmpp_on_message_groupchat",
                PRESENCE: "chat_xmpp_on_presence"
            }
        }
    };

    // Indicates whether user has joined room.
    chat.joined = false;

    // User's nickname.
    chat.nickname = null;

    // Jabber namespaces used by chat.
    chat.ns = {
        MUC: "http://jabber.org/protocol/muc"    // Multi-user chat.
    };

    // List of members that are presently participating in chat.
    chat.participants = {};

    // Help room's JID.
    chat.room = null;

    function formatBotResponse(jsontext) {
        var response = JSON.parse(jsontext);
        console.log('bot response', response);
        if (response.name == "utterance")
            return response.args[0];
    }
    
    // Prepares layout and binds events once DOM is ready.
    $(document).ready(function() {
        // Set up layout for window.
        $("body")
            .removeClass("hidden")
            .layout({
                east: {
                    size: "15%"
                },
                south: {
                    onresize: function(pane, $this) {
                        $this.trigger("resize");
                    }
                }
            });

        // Set up layout for members list.
        $("#chat-members").layout({
            north: {
                closable: false,
                resizable: false
            },
            south: {
                closable: false,
                resizable: false,
                size: 30
            }
        });

        // Listen for disconnect requests.
        $("#chat-disconnect").click(function(e) {
            // Prevent submitting of form.
            e.preventDefault();
            
            // Disable button.
            $(this)
                .attr("disabled", "disabled")
                .text("Disconnecting...");
            
            // Notify of disconnect from XMPP request.
            $(document).trigger(chat.events.xmpp.DISCONNECT);
        });

        // Listens for resizes of the chat message pane, then force a resize.
        $("#chat-message")
            .resize(function() {
                $("#chat-message-input").outerHeight($(this).innerHeight());
            })
            .trigger("resize");

        // Listen for key presses in textarea.
        $("#chat-message-input").keypress(function(e) {
            // Send message if the user hit Enter.
            if (e.which === 13) {
                var body = JSON.stringify({name:'utterance', args:[$(this).val()]});
                 console.log('sending', body);
                // Prevent Enter from being sent in message.
                e.preventDefault();

                if (body !== "") {
                    // Send message to chatbot.
                    chat.connection.send(
                        Strophe.$msg({
                            to: chat.room,
                            type: "groupchat"
                        }).c("body").t(body)
                    );

                    // Empty textarea.
                    $(this).val("");
                }
            }
        });

        // Listen for mouse hovering over message bodies.
        $(".message > .body").live("mouseenter mouseleave", function() {
            // Show timestamp when hovering over message.
            $(this).parent().find(".sender > .timestamp").stop().toggleClass("hidden");
        });

        // Lastly, display a dialog box asking the user for his/her name.
        $("#login-dialog").dialog({
            autoOpen: true,
            closeOnEscape: false,
            draggable: false,
            modal: true,
            resizable: false,
            title: "Welcome!",
            width: 400,
            buttons: {
                "Connect": function() {
                    // Store requested nickname.
                    chat.nickname = $("#login-nickname").val();

                    // Notify of connect to XMPP request.
                    $(document).trigger(chat.events.xmpp.CONNECT);

                    // Close dialog.
                    $(this).dialog("close");
                }
            }
        });
    });

    // Listens for new messages to add to chat.
    $(document).bind(chat.events.ADD_MESSAGE, function(e, $message) {
        var chat_window = $("#chat-window").get(0),
            at_bottom = chat_window.scrollTop >= chat_window.scrollHeight - chat_window.clientHeight;

        // Append chat message to window.
        $(chat_window).append($message);

        if (at_bottom) {
            // Ensure we are at the bottom if we had been previously.
            chat_window.scrollTop = chat_window.scrollHeight;
        }
    });

    // Listens for XMPP connects.
    $(document).bind(chat.events.xmpp.CONNECT, function() {
        var connection = new Strophe.Connection("/http-bind/");

        // Notify of connection status.
        connection.connect("olsportaldev", null, function(status) {
           if (status === Strophe.Status.CONNECTED) {
               $(document).trigger(chat.events.xmpp.CONNECTED);
           }
           else if (status === Strophe.Status.DISCONNECTED) {
               $(document).trigger(chat.events.xmpp.DISCONNECTED);
           }
        }, 50);

        // Store connection in namespace.
        chat.connection = connection;
    });

    // Listens for connect to XMPP requests.
    $(document).bind(chat.events.xmpp.CONNECTED, function() {
        // Disable receiving of private messages.
        chat.connection.send(Strophe.$pres().c("priority").t("-1"));

        // Add handler to listen for presence updates.
        chat.connection.addHandler(function(presence) {
            $(document).trigger(chat.events.xmpp.on.PRESENCE, presence);
            return true;
        }, null, "presence");

        // And another handler to listen for group chat messages.
        chat.connection.addHandler(function(message) {
            $(document).trigger(chat.events.xmpp.on.GROUPCHAT, message);
            return true;
        }, null, "message", "groupchat");

        // Request to join room.
        chat.connection.send(
            Strophe.$pres({
                to: chat.room + "/" + chat.nickname
            }).c("x", {
                xmlns: chat.ns.MUC
            })
        );
    });

    // Listens for disconnect from XMPP requests.
    $(document).bind(chat.events.xmpp.DISCONNECT, function() {
        // Update presence if presently in room.
        if (chat.connection.connected) {
            chat.connection.send(
                Strophe.$pres({
                    to: chat.room + "/" + chat.nickname,
                    type: "unavailable"
                })
            );
        }

        // Close connection.
        chat.connection.disconnect();
    });

    // Listens for XMPP disconnects.
    $(document).bind(chat.events.xmpp.DISCONNECTED, function() {
        // Empty elements on page.
        chat.connection = null;
        chat.joined = false;
        $("#chat-members-list, #chat-message-input").empty();
        
        // Disable textarea and disconnect button.
        $("#chat-message-input, #chat-disconnect").attr("disabled", "disabled");
        
        // Update text on disconnect button.
        $("#chat-disconnect").text("Disconnect");
        
        // Show login dialog again.
        $("#login-dialog").dialog("open");
    });

    // Listens for user joining room.
    $(document).bind(chat.events.xmpp.JOINED, function() {
        // Indicate that user has joined room.
        chat.joined = true;
        
        // Enable textarea and disconnect button.
        $("#chat-message-input, #chat-disconnect").removeAttr("disabled");
    });

    // Listens for group chat messages.
    $(document).bind(chat.events.xmpp.on.GROUPCHAT, function(e, message) {
        var $message = $(message),
            from = $message.attr("from"),
            room = Strophe.getBareJidFromJid(from),
            nick = Strophe.getResourceFromJid(from);

        // Ensure the group chat message is from the right room.
        if (chat.room === room) {
            var notice = !nick,
                body = $message.children("body").text(),
                now = new Date(),
                $node;

            if (!notice) {
                // Clone message template if not a notice.
                $node = $("#message-template")
                    .clone()
                    .removeAttr("id")
                    .removeClass("hidden")
                    .addClass(chat.nickname === nick ? "user" : "bot")
                    .corner("4px")
                    .find(".sender")
                        .corner("round tr br 4px")
                        .find(".username")
                            .corner("round tr br 4px")
                            .text(nick)
                            .end()
                        .find(".timestamp")
                            .text(now.toLocaleDateString() + " " + now.toLocaleTimeString())
                            .end()
                        .end()
                    .find(".body")
                        .text(formatBotResponse(body))
                        .end();
            }
            else if (!chat.joined && $message.find("status[code=100]").length) {
                if ($message.find("status[code=210]").length) {
                    // Store user's new nickname.
                    chat.nickname = Strophe.getResourceFromJid(from);
                }

                // Notify of user joining room.
                $(document).trigger(chat.events.xmpp.JOINED);

                // Stop processing further.
                return;
            }
            else {
                // Otherwise create a notice node.
                $node = $("<div></div>")
                    .addClass("notice")
                    .text("***" + body + "***");
            }

            // Add message node.            
            $(document).trigger(chat.events.ADD_MESSAGE, $node);
        }
    });

    // Listens for presence updates.
    $(document).bind(chat.events.xmpp.on.PRESENCE, function(e, presence) {
        var $presence = $(presence),
            from = $presence.attr("from"),
            room = Strophe.getBareJidFromJid(from);

        // Ensure the presence update is from the right room.
        if (chat.room === room) {
            var nick = Strophe.getResourceFromJid(from);

            if ($presence.attr("type") === "error" && !chat.joined) {
                // Reset application as there was an error joining room.
                $(document).trigger(chat.events.xmpp.DISCONNECT);
            } else if (!chat.participants[nick] && $presence.attr("type") !== "unavailable") {
                // Add participant to list.
                chat.participants[nick] = true;
                $("<li></li>").text(nick).appendTo("#chat-member-list");
            }
        }
    });

    // Get JID of chatbot's help room.
    (function() {
        $.getJSON("/chat/room").success(function(json) {
            chat.room = json.jid;
        });
    })();
});
