"use strict";
var page = require('webpage').create(),
system = require('system'),
pageWidth = 1067,
pageHeight = 473,
address, output;

if (system.args.length < 3 || system.args.length > 3) {
  console.log('Usage: screenshot.js <URL> <output filename>');
  phantom.exit(1);
} else {
  address = system.args[1];
  output = system.args[2];
  page.viewportSize = { width: pageWidth, height: pageHeight };
  //page.zoomFactor = 0.75;

  page.open(address, function (status) {
    if (status !== 'success') {
      console.log('Unable to load the address!');
      phantom.exit(1);
      return;
    }
    sleepAndTry();
  });
}
function sleepAndTry() {
  window.setTimeout(function () {
    var vaadinReady = page.evaluate(isVaadinReady);
    console.log("vaadin:");
    console.log(vaadinReady);
    if (vaadinReady) {
      // disable CSS scaling as it doesn't seem to work in PhantomJS
      page.evaluate(disableScaling);
      page.evaluate(hideLoadingIndicators);
      window.setTimeout(function () {
        page.render(output);
        phantom.exit();
      }, 20);
    }
    else {
      sleepAndTry();
    }
  }, 200);
}
function isVaadinReady() {
  var isVaadinWindowReady = function(vaadinWindow) {
    if (vaadinWindow.vaadin == null) {
      return true;
    }
    var clients = vaadinWindow.vaadin.clients;
    if (clients) {
      for (var client in clients) {
        if (clients[client].isActive()) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
  if (isVaadinWindowReady(window)) {
    var frame = document.getElementById("result-frame").getElementsByTagName("iframe")[0];
    return isVaadinWindowReady(frame.contentWindow);
  }
  return false;
}
function disableScaling() {
  var pView = document.getElementById("preview-view");
  pView.classList.remove("scale-down");
}
function hideLoadingIndicators() {
  var indicator = document.getElementsByClassName("v-loading-indicator")[0];
  indicator.style.top = "-100px";
  var frame = document.getElementById("result-frame").getElementsByTagName("iframe")[0];
  console.log(frame);
  var frameIndicator = frame.contentDocument.getElementsByClassName("v-loading-indicator")[0];
  console.log(frameIndicator);
  frameIndicator.style.top = "-100px";
}
