Leaflet 1.9.4, BSD 2-Clause license (see LICENSE).
Official project: https://leafletjs.com/download.html
Files: https://unpkg.com/leaflet@1.9.4/dist/leaflet.js and leaflet.css
SHA-256 (base64):
leaflet.js: 20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=
leaflet.css: p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=

The library and stylesheet are bundled; only visible TomTom Orbis v2 raster tiles use network.
Endpoint: https://api.tomtom.com/maps/orbis/display/raster/tile/{z}/{x}/{y}
Native WebView interception adds TomTom-Api-Key and TomTom-Api-Version: 2 headers.
Credentials never appear in the page, JavaScript, page URL or image URLs.
TomTomClient also uses Orbis v2 Geocoding for committed customer/outlet addresses.
No direct OpenStreetMap tile requests or Android Geocoder are used.
The OpenStreetMap attribution is required by TomTom Orbis data licensing.
Copyright notice: https://docs.tomtom.com/tomtom-orbis-maps/documentation/copyright
No default marker images/layer control are used. The pin is a local CSS div icon.
pickup-map.html is application code and has no user text interpolated into scripts.
