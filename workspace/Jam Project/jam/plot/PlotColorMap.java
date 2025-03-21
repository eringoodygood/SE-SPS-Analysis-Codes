package jam.plot;
import java.awt.*;
/**
 * Color map for display
 * The possible colors are
 *
 *@author Ken Swartz and Dale Visser
 *
 */
class PlotColorMap {

	private static final Color DARK_RED=new Color(192,0,0);
    public final static int BLACK_ON_WHITE=0;
    public final static int WHITE_ON_BLACK=1;
    public final static int PRINT=5;

    private final static int NUMBER_COLORS=9;

    static int colorMode;

    static Color colorScale[]=new Color [NUMBER_COLORS];
    static Color background;
    static Color foreground;
    static Color hist;
    static final Color [] overlay={Color.RED, Color.GREEN, Color.BLUE,
    Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.ORANGE, DARK_RED};
    static Color gateDraw;
    static Color gateShow;
    static Color fitTotal;
    static Color fitSignal;
    static Color fitBackground;
    static Color fitResidual;
    static Color mark;
    static Color area;
    static Color peakLabel;

    public  PlotColorMap(int mode){
        setColorMap(mode);
    }

    static void setColorMap(int mode){
        if (mode==BLACK_ON_WHITE){
            colorMode=mode;
            background=Color.WHITE;
            foreground=Color.DARK_GRAY;
            hist=Color.BLACK;
            gateDraw=Color.GREEN;
            gateShow=Color.RED;
            mark=Color.RED;
            area=Color.GREEN;
            fitTotal=Color.BLUE;
            fitSignal=Color.DARK_GRAY;
            fitBackground=Color.GREEN;
            fitResidual=Color.RED;
            colorScale=colorScaleBonW;
            peakLabel=Color.BLUE;
        } else if (mode==WHITE_ON_BLACK){
            colorMode=mode;
            background=Color.BLACK;
            foreground=Color.LIGHT_GRAY;
            hist=Color.WHITE;
            gateShow=Color.RED;
            gateDraw=Color.GREEN;
            mark=Color.YELLOW;
            area=Color.GREEN;
			fitTotal=Color.CYAN;
			fitSignal=Color.LIGHT_GRAY;
			fitBackground=Color.GREEN;
			fitResidual=Color.RED;
			peakLabel=Color.CYAN;
            colorScale=colorScaleWonB;
        } else if (mode==PRINT){
            colorMode=mode;
            background=Color.WHITE;
            foreground=Color.BLACK;
            hist=Color.BLACK;
            gateDraw=new Color(59, 59, 59);
            gateShow=new Color(59, 59, 59);
            mark=new Color(102, 102, 102 );
            area=new Color(102, 102, 102 );
			fitTotal=Color.BLUE;
			fitSignal=Color.DARK_GRAY;
			fitBackground=Color.GREEN;
			fitResidual=Color.RED;
			peakLabel=Color.BLUE;
            colorScale=colorScaleGray;
        } else {
            throw new IllegalArgumentException("PlotGraphicsColorMap.setColorMap("+mode+"): Invalid Color Mode!");
        }
    }

    /**
     * color scale
     */
    static Color [] getColorScale(){
        return colorScale;
    }
    
    /**
     * Number of colors
     */
    static int getNumberColors() {
        return NUMBER_COLORS;
    }
    
    private static Color [] colorScaleBonW= {
        new Color(0, 0, 127),  //0
        new Color(0, 0, 255),  //1
        new Color(128, 0, 255 ),  //2
        new Color(255, 0,255 ),  //3
        new Color(255,0,128  ),  //4
        new Color(255,0,0 ),  //5
        new Color(255,128,0 ),  //6
        new Color(255,255,0 ),  //7
        new Color(0  , 0  , 0 )  //9
    };

    private static Color [] colorScaleWonB= {
        new Color(0, 0, 127),  //0
        new Color(0, 0, 255),  //1
        new Color(128, 0, 255 ),  //2
        new Color(255, 0,255 ),  //3
        new Color(255,0,128  ),  //4
        new Color(255,0,0 ),  //5
        new Color(255,128,0 ),  //6
        new Color(255,255,0 ),  //7
        new Color(255, 255, 255 ),  //8
    };


    //color map for printing
    private static Color [] colorScaleGray= {
        new Color(0, 127, 0),  //0
        new Color(0, 0, 255),  //1
        new Color(128, 0, 255 ),  //2
        new Color(255, 0,255 ),  //3
        new Color(255,0,128  ),  //4
        new Color(255,0,0 ),  //5
        new Color(255,128,0 ),  //6
        new Color(255,255,0 ),  //7
        new Color(255, 255, 255 ),  //8
    };    
}
