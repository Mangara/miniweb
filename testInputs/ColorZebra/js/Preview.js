(function( ColorZebra, $, undefined ) {
    ColorZebra.Preview = function(theCanvas) {
        var canvas = theCanvas;
        var PI_BY_FOUR = Math.PI / 4;
        
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
            
            drawPiecewiseLinear();
        }
        
        function drawPiecewiseLinear() {
            // The test image consists of a sine wave plus a ramp function
            // The sine wave has a wavelength of 8 pixels (which is why we multiply by 2pi/8 = pi/4)
            // The base sine wave has amplitude 0.05, so that it spans 10% of the value range
            // In each column, the amplitude of the sine wave ranges from 0 at the bottom to 0.05 at the top, increasing quadratically
            // In each row, the ramp goes from <z> on the left to (1 - <z>) on the right, where <z> = 0.05 * ((height - y) / height)^2 is the maximum amplitude of the sine wave in that row
            // Drawing it per-pixel is slow, because context fillstyle changes are very expensive (much more than any calculations we're doing).
            // This method draws the same image, except that the amplitude of the sine wave approximates the quadratic modulation with a piecewise linear one.
            // This is ~25 times faster than drawing each pixel for STEPS = 10, and nearly impossible to distinguish visually.
            var STEPS = 10;
            
            var context = canvas.getContext("2d"),
                x,
                width = canvas.width,
                height = canvas.height;
                        
            for (x = 0; x < width; x++) {
                var xt = x / (width - 1); // x mapped to [0, 1]
                var sinVal = Math.sin(x * PI_BY_FOUR);
                
                var my_gradient = context.createLinearGradient(0, 0, 0, height);
                
                for (y = STEPS; y > 0; y--) {
                    var yt = y / STEPS;
                    var amp = 0.05 * yt * yt;
                    var val = amp * sinVal + getRamp(xt, amp);
                    my_gradient.addColorStop(1 - yt, ColorZebra.colorMap.getCSSColor(val));
                }
                
                context.fillStyle = my_gradient;
                context.fillRect(x, 0, 1, height);
            }
        }
        
        function drawQuadratic() {
            // Super-slow way of drawing the test image, for performance comparison
            
            var context = canvas.getContext("2d"),
                x, y,
                width = canvas.width,
                height = canvas.height;
                        
            for (x = 0; x < width; x++) {
                var xt = x / (width - 1); // x mapped to [0, 1]
                var sinVal = Math.sin(x * PI_BY_FOUR);
                
                for (y = 0; y < height; y++) {
                    var yt = (height - y) / (height - 1);
                    var amp = 0.05 * yt * yt;
                    var val = amp * sinVal + getRamp(xt, amp);
                    context.fillStyle = ColorZebra.colorMap.getCSSColor(val);
                    context.fillRect(x, y, 1, 1);
                }
            }
        }
        
        function getRamp(xt, amp) {
            return amp + (1 - 2 * amp) * xt;
        }
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));