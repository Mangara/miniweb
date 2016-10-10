(function( ColorZebra, $, undefined ) {
    ColorZebra.exportJSCode = function() {
        var result = "";

        // Function declaration
        result += "/* Returns a CSS color string for any value between 0 and 1 (inclusive).\n   Perceptive brightness increases monotonically from 0 to 1. */\n";
        result += "function getColor_" + ColorZebra.colorMap.name + "(value) {\n";

        // Bounds check
        result += "    if (value < 0 || value > 1) {\n"
                + "        return null;\n"
                + "    }\n\n"

        // Normalization
        var points = ColorZebra.colorMap.getControlPoints();

        if (points[0][0] == 0) {
            result += "    var L = value * " + points[points.length - 1][0] + ", A, B;\n\n"
        } else {
            result += "    var L = " + points[0][0] + " + value * " + (points[points.length - 1][0] - points[0][0]) + ", A, B;\n\n"
        }

        // Boundary case
        result += "    if (L == " + points[points.length - 1][0] + ") {\n"
                + "        A = " + points[points.length - 1][1] + ";\n"
                + "        B = " + points[points.length - 1][2] + ";\n"
                + "    } else {\n"

        // Spline evaluation
        if (ColorZebra.colorMap.getSpline() instanceof ColorZebra.LinearSpline) {
            result += "        var points = " + points + ";\n"
                    + "        var start = 0, end = " + (points.length - 1) + ";\n\n"
            
                    + "        while (end - start > 1) {\n"
                    + "            var middle = Math.floor(start + (end - start) / 2);\n"
                    + "            var midVal = points[middle][0];\n\n"
            
                    + "            if (midVal <= L) {\n"
                    + "                start = middle;\n"
                    + "            } else {\n"
                    + "                end = middle;\n"
                    + "            }\n"
                    + "        }\n\n"
            
                    + "        var fraction = (L - points[start][0]) / (points[end][0] - points[start][0]);\n"
                    + "        A = points[start][1] + fraction * (points[end][1] - points[start][1]);\n"
                    + "        B = points[start][2] + fraction * (points[end][2] - points[start][2]);\n\n"
        } else if (ColorZebra.colorMap.getSpline() instanceof ColorZebra.QuadraticSpline) {
            
        }

        // LAB to RGB conversion
        result += "    var fY = 0.00862068965 * (L + 16);\n"
                + "    var X = 0.95047 * f_inverse(fY + 0.002 * A);\n"
                + "    var Y =           f_inverse(fY);\n"
                + "    var Z = 1.08883 * f_inverse(fY - 0.005 * B);\n\n"

                + "    var R = Math.max(0, Math.min(1, correctGamma( 3.2406 * X - 1.5372 * Y - 0.4986 * Z)));\n"
                + "    var G = Math.max(0, Math.min(1, correctGamma(-0.9689 * X + 1.8758 * Y + 0.0415 * Z)));\n"
                + "    var B = Math.max(0, Math.min(1, correctGamma( 0.0557 * X - 0.2040 * Y + 1.0570 * Z)));\n\n"

                + "    return 'rgb(' + Math.round(255 * R) + ',' + Math.round(255 * G) + ',' + Math.round(255 * B) + ')';\n\n"

                + "    function f_inverse(t) {\n"
                + "        if (t > 0.20689655172) {\n"
                + "            return Math.pow(t, 3);\n"
                + "        } else {\n"
                + "            return 0.12841854934 * (t - 0.13793103448);\n"
                + "        }\n"
                + "    }\n\n"

                + "    function correctGamma(t) {\n"
                + "        if (t <= 0.0031308) {\n"
                + "            return 12.92 * t;\n"
                + "        } else {\n"
                + "            return 1.055 * Math.pow(t, 0.41666666666) - 0.055;\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        return result;
    }

    ColorZebra.exportJavaCode = function() {
        
    }


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
        
        for (i = 0; i < ColorZebra.numColors; i++) {
            color = ColorZebra.Color.LABtoRGB(colors[i]);
            result += '<color name="' + ColorZebra.colorMap.name + '_' + i + '" value="' + color[0] + ' ' + color[1] + ' ' + color[2] + '"/>\n';
        }
        
        result += '</ipestyle>';
        
        return result;
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));