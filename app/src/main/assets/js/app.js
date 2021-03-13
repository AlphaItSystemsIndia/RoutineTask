$(document).ready(function () {
    // Obtain Section References
    const eulaSection = $("#eula");
    const policySection = $("#policy");
    const developerSection = $("#developer");
    const socialSection = $("#social");
    const ackSection = $("#acknowledgement");

    // Register Internal Link Events
    $("#eulaBtn").on('click touch', function (e) {
        e.preventDefault();
        $('html, body').animate({
            scrollTop: eulaSection.offset().top
        }, 1000);
    })
    $("#policyBtn").on('click touch', function (e) {
        e.preventDefault();
        $('html, body').animate({
            scrollTop: policySection.offset().top
        }, 1000);
    });
    $("#devBtn").on('click touch', function (e) {
        e.preventDefault();
        $('html, body').animate({
            scrollTop: developerSection.offset().top
        }, 1000);
    });
    $("#socialBtn").on('click touch', function (e) {
        e.preventDefault();
        $('html, body').animate({
            scrollTop: socialSection.offset().top
        }, 1000);
    });
    $("#ackBtn").on('click touch', function (e) {
        e.preventDefault();
        $('html, body').animate({
            scrollTop: ackSection.offset().top
        }, 1000);
    });

    // Floating Button Behaviour
    const floatingBtn = $("#floatingBtn");
    floatingBtn.on('click touch', function (e) {
        e.preventDefault();
        $('html, body').animate({
            scrollTop: $('#main').offset().top
        }, 1500);
    });

    // Hide floating Button initially
    floatingBtn.fadeOut(0);
    $(window).scroll(function () {
        var scrollPos = window.pageYOffset || document.documentElement.scrollTop;
        if (scrollPos > (window.innerHeight)) {
            floatingBtn.fadeIn(300);
        } else {
            floatingBtn.fadeOut(300);
        }
    });

});
