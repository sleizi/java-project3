module security.service {
    requires java.desktop;
    requires image.service;
    requires miglayout.swing;
    requires java.prefs;
    requires com.google.gson;
    requires guava;
    opens com.udacity.catpoint.data to com.google.gson;
}