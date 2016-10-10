(function( ColorZebra, $, undefined ) {
    ColorZebra.FixedNumPreview = function(theCanvas) {
        var canvas = theCanvas;
        
        this.maximize = function() {
            var parent = $(canvas).parent();
            canvas.width = parent.width();
            canvas.height = parent.height() - 4; // No clue why the -4 is necessary.
        }
        
        this.draw = function() {
            if (canvas === undefined) {
                alert('Canvas undefined');
                return;
            }
            
            var context = canvas.getContext("2d"),
                i,
                colWidth = canvas.width / ColorZebra.numColors,
                start, end;
                
            // Clear the canvas
            context.clearRect(0, 0, canvas.width, canvas.height);
            
            if (colWidth > 1) {
                // Draw individual colors
                for (i = 0; i < ColorZebra.numColors; i++) {
                    if (colWidth >= 10) {
                        // Discrete full-pixel boundaries
                        start = Math.floor(i * colWidth);
                        end = Math.floor((i + 1) * colWidth) - 1;
                    } else {
                        // Aliased relative boundaries
                        start = i * colWidth;
                        end = (i + 0.9) * colWidth;
                    }
                    
                    var val = i / (ColorZebra.numColors - 1);
                    
                    context.fillStyle = ColorZebra.colorMap.getCSSColor(val);
                    context.fillRect(start, 0, end - start, canvas.height);
                }
            } else {
                // Just draw the colormap
                for (i = 0; i < canvas.width; i++) {
                    var val = i / (canvas.width - 1);
                    
                    context.fillStyle = ColorZebra.colorMap.getCSSColor(val);
                    context.fillRect(i, 0, 1, canvas.height);
                }
            }
        }
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));