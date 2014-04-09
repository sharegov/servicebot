/*
 * main.js
 *
 * Copyright (c) 2011 Miami-Dade County. All rights reserved.
 */
"use strict";

define(["jquery", "jqtouch", "events", "platform", "strophe", "xmpp"], function ($, jQTouch, events, platform, strophe, xmpp) {
    var jabberd = "w203-007",
        jqt$ = jQTouch(),
        Strophe = strophe.Strophe,
        $build = strophe["$build"],
        $iq = strophe["$iq"],
        $msg = strophe["$msg"],
        $pres = strophe["$pres"],
        lastNick = null;
    
    $(function () {
        var $hud = $("#hud"),
            $toolbar = $("#chat .toolbar"),
            reply = function () {
                $(this).unbind(platform.click);
                $hud
                    .offset({
                        top: ($toolbar.offset().top + $toolbar.outerHeight())
                    })
                    .removeClass("collapsed")
                    .find("input[type='text']")
                        .height($("#chat-content").outerHeight())
                        .width($("#chat-content").outerWidth() - 70)
                        .focus();
            };
        
        xmpp.nickname = "me";
        
        $("#chat").bind("pageAnimationStart", function (e, animation) {
            if (animation.direction === "in") {
                $(document).trigger(events.xmpp.CONNECT, $("#chat"));
            }
            else if (animation.direction === "out") {
                if (!$("#hud").hasClass("collapsed")) {
                    $("#hud").addClass("collapsed");
                }
                
                $(document).trigger(events.xmpp.DISCONNECT)
            }
        });
        
        $("#reply-button").bind(platform.click, reply);
        $("#chat-message").keypress(function (e) {
            // Send message if the user hit Enter.
            if (e.which === 13) {
                var body = $(this).val();
                
                // Prevent Enter from being sent in message.
                e.preventDefault();
                
                if (body !== "") {
                    // Send message to chatbot.
                    xmpp.connection.send(
                        $msg({
                            to: xmpp.room,
                            type: "groupchat"
                        }).c("body").t(body)
                    );
                    
                    // Empty textarea.
                    $(this).val("");
                    
                    // Collapse chat window.
                    $hud.addClass("collapsed");
                    $("#reply-button").bind(platform.click, reply);
                }
            }
        });
        
        $(document).trigger(events.xmpp.jid.GET);
    });
    
    $(document).bind(events.message.ADD, function (e, $message) {
        var chat_window = $("#chat-content").get(0),
            at_bottom = chat_window.scrollTop >= chat_window.scrollHeight - chat_window.clientHeight;
        
        // Append chat message to window.
        $(chat_window).append($message);
        
        if (at_bottom) {
            // Ensure we are at the bottom if we had been previously.
            chat_window.scrollTop = chat_window.scrollHeight;
        }
    });
    
    $(document).bind(events.xmpp.jid.GET, function () {
        $.getJSON("/chat/room")
            .success(function (room) {
                xmpp.room = room.jid;
            })
            .error(function () {
                $.error("Failed to connect to server.");
            });
    });
    
    $(document).bind(events.xmpp.on.GROUPCHAT, function (e, message) {
        var $message = $(message),
            from = $message.attr("from"),
            room = Strophe.getBareJidFromJid(from),
            nick = Strophe.getResourceFromJid(from);
        
        // Ensure the group chat message is from the right room.
        if (xmpp.room === room) {
            var notice = !nick,
                body = $message.children("body").text(),
                now = new Date(),
                $node;
            
            if (!notice) {
                $node = $("<p></p>")
                    .addClass("chat-bubble")
                    .addClass(xmpp.nickname === nick ? "right" : "left")
                    .html(body);
            }
            else if (!xmpp.joined && $message.find("status[code=100]").length) {
                if ($message.find("status[code=210]").length) {
                    // Store user's new nickname.
                    chat.nickname = Strophe.getResourceFromJid(from);
                }
                
                // Notify of user joining room.
                $(document).trigger(events.xmpp.JOINED);
                
                // Stop processing further.
                return;
            }
            else {
                // TODO: Otherwise create a notice node.
            }
            
            // Add message node.
            $(document).trigger(events.message.ADD, $node);
        }
    });
    
    $(document).bind(events.xmpp.CONNECT, function () {
        var connection = new Strophe.Connection("/http-bind/");
        
        // Notify of connection status.
        connection.connect(jabberd, null, function (status) {
           if (status === Strophe.Status.CONNECTED) {
               $(document).trigger(events.xmpp.CONNECTED);
           }
           else if (status === Strophe.Status.DISCONNECTED) {
               $(document).trigger(events.xmpp.DISCONNECTED);
           }
        }, 50);
        
        // Store connection.
        xmpp.connection = connection;
    });
    
    $(document).bind(events.xmpp.CONNECTED, function () {
        // Disable receiving of private messages.
        xmpp.connection.send($pres().c("priority").t("-1"));
        
        // Add handler to listen for presence updates.
        xmpp.connection.addHandler(function (presence) {
            $(document).trigger(events.xmpp.on.PRESENCE, presence);
            return true;
        }, null, "presence");
        
        // And another handler to listen for group chat messages.
        xmpp.connection.addHandler(function (message) {
            $(document).trigger(events.xmpp.on.GROUPCHAT, message);
            return true;
        }, null, "message", "groupchat");
        
        // Request to join room.
        xmpp.connection.send(
            $pres({
                to: xmpp.room + "/" + xmpp.nickname
            }).c("x", {
                xmlns: xmpp.ns.MUC
            })
        );
    });
    
    $(document).bind(events.xmpp.DISCONNECT, function () {
        // Update presence if presently in room.
        if (xmpp.connection.connected) {
            xmpp.connection.send(
                $pres({
                    to: xmpp.room + "/" + xmpp.nickname,
                    type: "unavailable"
                })
            );
        }
        
        // Close connection.
        xmpp.connection.disconnect();
    });
    
    $(document).bind(events.xmpp.DISCONNECTED, function () {
        console.log("Disconnected...");
    });
});
