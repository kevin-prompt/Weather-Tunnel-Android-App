The Weather Tunnel is a hobby project created between jobs a few years ago.  The aim of the project was to get up to speed on Android and provide (me) a simple, data focused weather app.  I actually still run this for myself and friends (it incurred some cost to run it for thousands of people).  

There are two parts.  This repo is the client component.  The other part is the Web Services.  The Android App has a simple UI, providing an initial list of places with their conditions, and allowing a user to drill down to get at detailed weather data and 10 day forecasts.  It touches on a large swath of Android programing techniques.  There is an action bar and settings page, it uses multiple types of activities and fragments, it is location aware and uses existing apps (contacts and maps) to extend its abilities.  It even has a widget.  

The data in the application is collected using web services.  While there are some direct calls to Google and the Weather Underground, mostly it uses the Weather Tunnel Web Services.  These all return xml, which was a bit more common when this was originally written.  The data is extracted with the help of an xml Pull parser and some assumptions on the layout of the xml.  

The project was ported from eclipse to Android Studio.  During the conversion the external libraries ActionBarSherlock and Admob were removed, as ads were not being served and the compatibility library could handle the action bar.

To run this App, you mostly need to build it in Android Studio and let it know the address of the Weather Tunnel API (see the Constants.java file).


