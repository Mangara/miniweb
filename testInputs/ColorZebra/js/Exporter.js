(function( ColorZebra, $, undefined ) {
    function getColors() {
        var colors = [], i;
        
        for (i = 0; i < ColorZebra.numColors; i++) {
            colors.push(ColorZebra.colorMap.getLABColor(i / (ColorZebra.numColors - 1)));
        }
        
        return colors;
    }
    
    ColorZebra.exportIntegerCSV = function() {
        var result = "r,g,b\r\n", i; // Always uses \r\n as per RFC 4180 ( https://tools.ietf.org/html/rfc4180 )
        var colors = getColors(),
            color;
        
        for (i = 0; i < ColorZebra.numColors; i++) {
            color = ColorZebra.Color.LABtoIntegerRGB(colors[i]);
            result += color[0] + ',' + color[1] + ',' + color[2] + '\r\n';
        }
        
        return result;
    }
    
    ColorZebra.exportFloatCSV = function() {
        var result = "r,g,b\r\n", i; // Always uses \r\n as per RFC 4180 ( https://tools.ietf.org/html/rfc4180 )
        var colors = getColors(),
            color;
        
        for (i = 0; i < ColorZebra.numColors; i++) {
            color = ColorZebra.Color.LABtoRGB(colors[i]);
            result += color[0] + ',' + color[1] + ',' + color[2] + '\r\n';
        }
        
        return result;
    }
    
    ColorZebra.exportIPE = function() {
        var result = '<ipestyle name="' + ColorZebra.colorMap.name + '_ColorMap_' + ColorZebra.numColors + '">\n'; // IPE understands \n, even on Windows
        var i;
        var colors = getColors();
        
        console.log('Colors: ' + colors);
        
        for (i = 0; i < ColorZebra.numColors; i++) {
            color = ColorZebra.Color.LABtoRGB(colors[i]);
            result += '<color name="' + ColorZebra.colorMap.name + '_' + i + '" value="' + color[0] + ' ' + color[1] + ' ' + color[2] + '"/>\n';
        }
        
        result += '</ipestyle>';
        
        return result;
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));