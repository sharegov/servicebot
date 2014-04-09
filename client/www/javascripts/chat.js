/*
 * chat.js
 *
 * Copyright (c) 2011 Miami-Dade County. All rights reserved.
 */
/*jshint indent: 4, white: false */
/*global define: false */

define(['jquery', 'strophe', 'encoder', 'bootstrap'], 
    function ($, Strophe, encoder) {
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
                GROUPCHAT: "chat_xmpp_on_message_groupchat"
            }
        }
    };

    // Indicates whether user has joined room.
    chat.joined = false;

    // User's nickname.
    chat.nickname = null;

    // Jabber name space used by chat.
    chat.ns = {
        MUC: "http://jabber.org/protocol/muc"    // Multi-user chat.
    };

    // List of members that are presently participating in chat.
    chat.participants = {};

    // Help room's JID.
    chat.room = null;
    
    // User
    chat.username = null;
    chat.auth = null;

    
    // Format messages to render in chat window
    function formatBodyMessage(jsontext) {        
        var response = JSON.parse(encoder.htmlDecode(jsontext));
        //console.log('format', response);
        if (response.name == "utterance" || response.name == "html")
            return response.args[0];
        else if (response instanceof Array) { 
            var text = "";
            $.each(response, function(i,x) {
                if (x.name == "form_field") {
                    var formel = $('#' + x.args[0])[0];
                    var value = (formel.tagName == "SELECT") ? 
                    		$('option[value="' + x.args[1] + '"]', $(formel)).html() : x.args[1];
                    if (text != "") { text = text + "," } ;
                    text = text + "<strong>" + value + "</strong>";
                }
            });
            return text;
        }
        else {
            return jsontext;
        }
    }
    
    // Check list updates
    function updateChecklist() {
    	$.getJSON("/chat/predicateValue/" + chat.room + "/checkList").success(function(json) {
	        if (json.ok && json.value) {
	        	// Set the checklist title
	        	var business = $("#chatWindow tr:nth-child(2) td:nth-child(2)").text();
	        	var title = "<strong>To open a <em>" + business + "</em> you must:</strong>";
	        	$('#checklistTitle').empty();
	        	$('#checklistTitle').append(title);
	        	
	            var list = $('#checkList').empty();
	            $.each(json.value, function(i, x) {
	                list.append($("<li/>").append(x.args[1].replace(/\\([^\\])/g, '\n$1')));
	            });
	            // make sure all links open in a  new page...
	            $.each($('a', list), function(i,el) 
	                {if (!$(el).attr("target")) $(el).attr("target", "_blank");});
	            $("#checklistSave").removeAttr("disabled");
	        }
	    });
    }
    
    // Save user's checklist
    function saveChecklist() {
    	$('#checklistSave').button('loading');
    	var todos = [];
    	
    	$('#checkList').children('li').each( function() {
    		todos.push($(this).html());
    	});
    	
    	var business = $("#chatWindow tr:nth-child(2) td:nth-child(2)").text();
    	var successDiv = '<div class="alert alert-success fade in"><button class="close" data-dismiss="alert">&times;</button><strong>';
    	var successClose = '</strong></div>';
    	
    	$.post(
    		"/user/save", 
    		{username: chat.username, business: business, checklist: JSON.stringify(todos)},
    		function(msg){ 
    			$('#alertBox').append(successDiv + msg + successClose);
    			$('#checklistSave').button('reset');
    			showChecklist();
    		}, 
    		"text"
    	);

    }
    
    // Show user's checklist
    function showChecklist() {
    	if (chat.auth == "true") {
    		$('#accordion').empty();
    		
			$.getJSON("/user/retrieve/" + chat.username, function(data) {
				$.each(data, function(index, val) {		
					// Clone the accordion template
					var node = $("#message-template")
					    .clone()
					    .removeAttr("id")
					    .removeClass("hide")
					    .find(".accordion-toggle")
					    	.attr('href', '#collapse' + index)
					    	.append(val.business + " (" + val.date + ")")
					    	.end()
					    .find(".accordion-body")
					    	.attr('id', 'collapse' + index)
					    	.end();
					
					// Add the checklist items
					var c = $.parseJSON(val.checklist);
				    $.each(c, function(i, v){
				    	$(node).find('ol').append('<li>' + v + '</li>');
				    });
				    
				    $('#accordion').append(node);
				  });
				
				$('#myChecklists').show();
			});
		}
    }
    
    // User sign in
    function userSignIn(event) {
    	event.preventDefault();
    	$('#invalid-login').hide();
    	$('#sigin-btn').attr("disabled", "disabled");
    	
    	var request = $.post(
    	  "/user/login", 
          {username : $('#username').val(), password : $('#password').val()},
          "json"
        );

    	request.done(function(msg) {
    	  chat.auth = msg.auth;
    	  chat.username = msg.username;
    	  //console.log(msg);
    	  //console.log(chat.auth);
    	  //console.log(chat.username);
    	  
    	  if (msg.auth == "true") {
    		  $('#create-account').hide();
    		  $('#signin-box').hide();
    		  $('#user-box').append('<li><a href="" onclick="return false;">' + msg.username + '</a></li>');
    		  $('#checklistSave').show();
    		  showChecklist();
    	  } else {
    		  $('#invalid-login').show();
    		  $("#sigin-btn").removeAttr("disabled");
    	  }
    	});

    	request.fail(function(jqXHR, textStatus) {
    		$('#invalid-login').show();
    		$("#sigin-btn").removeAttr("disabled");
    	});
    }
        
    // Prepares layout and binds events once DOM is ready.
    $(document).ready(function() {
    	
        $("table").delegate("form", "submit", function(event) {
        	event.preventDefault();
        	$('#loading').show();
        	
        	var fields = $(this).serializeArray();
        	fields = $.map(fields, function(el) {
        		return {name:'form_field', args:[el.name, el.value]};
        	});
        	
        	var body = JSON.stringify(fields);
            
            // Disable form and remove active form marker
        	$(this).find("select").attr("disabled", "disabled");
        	$(this).find("input").attr("disabled", "disabled");
            $("#activeForm").removeAttr("id");

            // Send message to OpenFire
            chat.connection.send(
                Strophe.$msg({
                    to: chat.room,
                    type: "groupchat"
                }).c("body").t(body)
            );
        });

        $("#myModal").on('hide', function() {
        	// Store requested nickname.
            chat.nickname = $("#nickname").val() !== "" ? $("#nickname").val() : "Guest";
            //console.log('nickname: ' + chat.nickname);
            $('body').scrollTop(0);
            $(document).trigger(chat.events.xmpp.CONNECT);
        });
        
        $("#checklistSave").click(function() {
        	saveChecklist();
        });
        
        $('#login').submit(function(event){
        	userSignIn(event);
        });
        
        /* Toolbar
        
        var toolBarState = new Persist.Store('Footer Toolbar');
        toolBarState.get('minimized', function(ok, val) {
          if(ok) {
            if(!val || val == 'false') {
              return false;
            } else {
              $("#floatingBar").slideToggle('slow', function()
              {$("#floatingBarOpen").slideToggle('slow')});
            }
          }
        });

        $("#min").click(function() {
          toolBarState.set('minimized', 'true');
          $("#floatingBar").slideToggle('slow', function(){
          $("#floatingBarOpen").slideToggle('slow')});   
          return false;
        });

        $("#max").click(function() {
          toolBarState.set('minimized', 'false');
          $("#floatingBarOpen").slideToggle('slow', function(){
          $("#floatingBar").slideToggle('slow')});
          return false;
        });
        
        $('#shareButton').click( function(event){
 		  event.preventDefault();
 		}); 
        */
    });

    // Listens for new messages to add to chat.
    $(document).bind(chat.events.ADD_MESSAGE, function(e, $message) {
    	// Hide loading message
    	if ($message.indexOf(">BizBot<") != -1) {
			$('#loading').hide();
		}
    	
    	$('#chatWindow').append($message.replace(/\\([^\\])/g, '\n$1'));
    	var bottom = $('#chatWell').prop('scrollHeight');
    	$('#chatWell').animate({scrollTop: bottom});
    	updateChecklist();
    });

    // Listens for XMPP connects.
    $(document).bind(chat.events.xmpp.CONNECT, function() {
        var connection = new Strophe.Connection("/http-bind/");

        // Notify of connection status.        
        connection.connect(document.location.hostname, null, function(status) {
           if (status === Strophe.Status.CONNECTED) {
               $(document).trigger(chat.events.xmpp.CONNECTED);
           }
           else if (status === Strophe.Status.DISCONNECTED) {
               $(document).trigger(chat.events.xmpp.DISCONNECTED);
           }
        }, 50);

        // Store connection in name space.
        chat.connection = connection;
    });

    // Listens for connect to XMPP requests.
    $(document).bind(chat.events.xmpp.CONNECTED, function() {
        // Disable receiving of private messages.
        chat.connection.send(Strophe.$pres().c("priority").t("-1"));

        // Handler to listen for group chat messages.
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

    // Listens for user joining room.
    $(document).bind(chat.events.xmpp.JOINED, function() {
        // Indicate that user has joined room.
        chat.joined = true;
    });

    // Listens for group chat messages.
    $(document).bind(chat.events.xmpp.on.GROUPCHAT, function(e, message) {
    	//console.log('GROUPCHAT : ' + message);
    	
        var $message = $(message),
            from = $message.attr("from"),
            room = Strophe.getBareJidFromJid(from),
            nick = Strophe.getResourceFromJid(from);

        //console.log($message);
        
        // Ensure the group chat message is from the right room.
        if (chat.room === room) {
            var notice = !nick,
                body = $message.children("body").text(),
                now = new Date(),
                $node;
            
            if (!notice) {
            	var handle = chat.nickname === nick ? 
            			'<span class="label label-info">' + nick + '</span>' : 
            			'<span class="label label-default">BizBot</span>';
            	
            	$node = "<tr><td>" + handle + "</td><td>" + formatBodyMessage(body) + "</td></tr>";
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
   return chat;
});
