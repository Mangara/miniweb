(function( ColorZebra, $, undefined ) {
    // Important variables with their initial values
    ColorZebra.colorMap = ColorZebra.colorMaps['Lake'];
    ColorZebra.numColors = 12;
    
    // Assign all action handlers at startup
    function assignActionHandlers() {
        // Change the active colormap when a thumbnail is clicked
        $('#colormaps>canvas').click(function() {
            var map = ColorZebra.colorMaps[this.id];
            
            if (ColorZebra.colorMap !== map) {
                // Saturate the thumbnail
                map.canvas.setDesaturate(false);
                map.canvas.draw();
                
                // Switch the selected class
                $('#colormaps>.selected').removeClass('selected');
                $(this).addClass('selected');
                
                // Desaturate the current thumbnail
                ColorZebra.colorMap.canvas.setDesaturate(true);
                ColorZebra.colorMap.canvas.draw();
                
                // Switch maps
                ColorZebra.colorMap = map;
                
                // Redraw stuff
                ColorZebra.mainPreview.draw();
                ColorZebra.fixedNumPreview.draw();
            }
        }).hover(function() { // Saturate hovered and focused thumbnails
            saturateThumbnail(this);
        }, function() {
            desaturateThumbnail(this);
        }).focus(function() {
            saturateThumbnail(this);
        }).blur(function() {
            desaturateThumbnail(this);
        }).keydown(function(e) { // React to key events when focused
            var code = e.which; // 13 = Enter, 32 = Space
            if ((code === 13) || (code === 32)) {
                $(this).click();
                return false; // Prevent the event from bubbling further
            }
        });
        
        function saturateThumbnail(thumbnail) {
            var canvas = ColorZebra.colorMaps[thumbnail.id].canvas;
            canvas.setDesaturate(false);
            canvas.draw();
        }
        
        function desaturateThumbnail(thumbnail) {
            var map = ColorZebra.colorMaps[thumbnail.id];
            
            if (ColorZebra.colorMap !== map) {
                map.canvas.setDesaturate(true);
                map.canvas.draw();
            }
        }
        
        // Update the number of colors
        $('#numcolors').keydown(function(e) {
            if (e.which === 13) {
                updateNumColors();
            }
        });
        
        $('#fixednum-apply').click(function() {
            updateNumColors();
        });
        
        function updateNumColors() {
            ColorZebra.numColors = $('#numcolors').val();
            ColorZebra.fixedNumPreview.draw();
        }
        
        // Make the download link work
        $('#download').click(function() {
            switch ($("#format").val()) {
                case 'csv-int':
                    download(this, 'csv', 'csv', ColorZebra.exportIntegerCSV());
                    break;
                case 'csv-float':
                    download(this, 'csv', 'csv', ColorZebra.exportFloatCSV());
                    break;
                case 'ipe':
                    download(this, 'plain', 'isy', ColorZebra.exportIPE());
                    break;
                case 'js-code':
                    download(this, 'application/javascript', 'js', ColorZebra.exportJSCode());
                    break;
                case 'java-code':
                    download(this, 'text/x-java-source', 'java', ColorZebra.exportJavaCode());
                    break;
            } 
        });
        
        function download(link, mimeType, extension, fileContents) {
            // Based on "download.js" v4.0, by dandavis; 2008-2015. [CCBY] see http://danml.com/download.html
            if (navigator.msSaveBlob) { // IE10 can't do a[download], only Blobs
                var blob = new Blob([fileContents], {type: 'text/' + mimeType});
                navigator.msSaveBlob(blob, 'colormap.' + extension);
            } else if ('download' in link) { // HTML5 a[download]
                link.href = 'data:text/' + mimeType + ';charset=utf-8,' + encodeURIComponent(fileContents);
                link.download = 'colormap.' + extension;
            } else if (typeof safari !== undefined) { // Handle non-a[download] safari as best we can:
				var url = 'data:application/octet-stream;charset=utf-8,' + encodeURIComponent(fileContents);
                
				if (!window.open(url)) { // Popup blocked, offer direct download:
					if (confirm('Displaying New Document\n\nUse Save As... to download, then click back to return to this page.')) { 
                        location.href=url;
                    }
				}
			} else { // None of these download options are supported. Just show the text and allow them to copy it.
                
            }
        }
        
        // Make our canvases respond to window resizing
        $(window).resize(function() {
            ColorZebra.mainPreview.maximize();
            ColorZebra.mainPreview.draw();
            ColorZebra.fixedNumPreview.maximize();
            ColorZebra.fixedNumPreview.draw();
        });
    }
    
    // Handle on-load stuff
    $(document).ready(function() {
        // Prepare our preview panels
        ColorZebra.mainPreview = new ColorZebra.Preview($('#preview')[0]);
        ColorZebra.mainPreview.maximize();
        ColorZebra.mainPreview.draw();
        
        ColorZebra.fixedNumPreview = new ColorZebra.FixedNumPreview($('#fixednum-preview')[0]);
        ColorZebra.fixedNumPreview.maximize();
        ColorZebra.fixedNumPreview.draw();
        
        // Create all thumbnails
        $('#colormaps>canvas').each(function() {
            var map = ColorZebra.colorMaps[this.id];
            map.canvas = new ColorZebra.CMapDrawer(this, map);
            
            if (map === ColorZebra.colorMap) {
                map.canvas.setDesaturate(false);
                $(this).addClass('selected');
            }
            
            map.canvas.draw();
            
            this.title = map.description;
        });
        
        assignActionHandlers();
    });
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));