(function( ColorZebra, $, undefined ) {
    ColorZebra.CMapDrawer = function(theCanvas, theColorMap) {
        var canvas = theCanvas;
        var colorMap = theColorMap;
        
        var desaturate = true;
        
        this.setDesaturate = function(newDesaturate) {
            desaturate = newDesaturate;
        }
        
        this.draw = function() {
            if (canvas === undefined) {
                alert('Canvas undefined');
                return;
            }
            
            var context = canvas.getContext("2d"),
                x,
                width = canvas.width,
                height = canvas.height;
                        
            for (x = 0; x < width; x++) {
                var val = x / (width - 1);
                context.fillStyle = getColor(val);
                context.fillRect(x, 0, 1, height);
            }
        }
        
        function getColor(val) {
            var color = colorMap.getLABColor(val);
            
            if (desaturate) {
                color = ColorZebra.Color.desaturateLAB(color);
            }
            
            return ColorZebra.Color.LABtoCSS(color);
        }
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));