/*
 * main.js
 *
 * Copyright (c) 2011 Miami-Dade County. All rights reserved.
 */
/*jshint indent: 4, white: false */
/*global require: false */
  
// Configure RequireJS
require.config({
  paths: {
      "U":    "http://sharegov.org/jslib/U",
      "rest":    "http://sharegov.org/jslib/rest",
  },
  priority: ['./jquery', './strophe']
  ,  urlArgs: "cache_bust=" + (new Date()).getTime()
});

// Require necessary modules
require(['./chat', "rest"], function (chat, rest) {
  // Global setup
  $.ajaxSetup({cache: false});
  // Parsing query string information
  window.QueryStringParameters={}; 
  var querystring = location.href.split('?')[1];
  if (querystring)  $.each(querystring.split('&'), function(i,v) { 
    var A = v.split('='); window.QueryStringParameters[A[0]] = A[1]; 
  });
  
  var rtop = new rest.Client("");
  // tmp for debug:
  window.rtop = rtop;
  
  var roomurl = "/chat/room";
  var context = null;
  // Get current chat context and initialize accordingly.
  if (QueryStringParameters.context) {
    roomurl += "?context=" + QueryStringParameters.context;
    context = rtop.get('/iframes/context/'+ QueryStringParameters.context);
  }
  else {
      context = rtop.get('/iframes/context/default');
  }
//  console.log('context', context);
  // Get JID of chatbot's help room.

  var jroom = rtop.get(roomurl, {}, function(x) {
          console.log('got back', x);
  });
  if (!jroom.ok) {
        console.log('Chat room creation error', jroom);
        alert('Unable to initiate chat, please try again later.');
        return;
   }
   else {
     chat.room = jroom.jid;
     //console.log('room: ' + chat.room)
     $('#myModal').modal('toggle');
  }
  
  // Apply UI settings
  if (context.configuration.ui) $.each(context.configuration.ui, function (name, value) {
    console.log('ui set', name, $('#' + name));
    $('#' + name).html(value);
  });
});
