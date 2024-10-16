package overkeys.generator;

import gui.GUIController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class OverKeysGenerator {

    Scanner scan = new Scanner(System.in);
    PrintWriter pw, pwKeyTop, pwClamp, pwValues, togetherPrint;
    double metalRoundRadius, octaveWidth, periodWidth, underKeyWidth, blackKeyHeight, blackKeyLength, whiteKeyHeight, whiteKeyLength, whiteKeyLengthPreShortening, edgeRadius = 3, keytopHeight = 10, tolerance = 0.05,//measurements
            genh, genhPreShortening, overhead, keyTopSide1, keyTopSide2, shiftX, shiftY, slantCutWidth,//derived stuff
            theta, q, r, a, aPreShortening, b, c, d, dPreShortening, z,//bunch of triangle stuff
            generator, keyScale, holeScaleX, holeScaleY, stalkScaleX, stalkScaleY, keytopHeightDifference,xToleranceGap,yToleranceGap,extraLever,prowFactor;
    double metalRoundRadiusTolerance = 0.0125, underKeyGap;
    int periodSteps, generatorSteps, desiredGamut, startingKey, range, genForLargeStep, genForSmallStep, stepsForLarge, stepsForSmall, genForStep1, genForStep1b;
    boolean isKeytop, verticalFlip, neededAbsoluteValue = false, manualKeyFlip, shiftXTrue, roughRender, keytopsInTogether, keytopsInSingleKeyFiles;
    double xSliderValue,ySliderValue;

    ArrayList<Integer> mosSizes = new ArrayList<>();
    ArrayList<mosScale> mosTracker = new ArrayList<>();

    String renderPath;


    public static void main(String[] args) {
        OverKeysGenerator IKG = new OverKeysGenerator();

        if (IKG.setConstantsFromConfig())
            IKG.setDefaultConstants();
        IKG.getUserInputAndDeriveConstants();
        IKG.generateFiles();
    }

    public void setRenderPath(String renderPath){
        this.renderPath=renderPath;
    }


    public boolean setConstantsFromConfig() {

        Map<String,Double> values=new HashMap<>();
        try (Scanner sc = new Scanner(new File("./resources/config.txt"))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] properties = line.split(GUIController.SEPARATOR, -1);
                values.put(properties[0],Double.parseDouble(properties[1]));
            }

            octaveWidth=values.get("octaveWidth");
            blackKeyHeight=values.get("blackKeyHeight");
            blackKeyLength=values.get("blackKeyLength");
            whiteKeyLengthPreShortening=values.get("whiteKeyLength");
            keytopHeightDifference=values.get("keytopHeightDiff");
            metalRoundRadius=values.get("metalRoundRadius");
            xToleranceGap=values.get("stalkFitXTolerance");
            yToleranceGap=values.get("stalkFitYTolerance");
            keyScale=values.get("keytopScale");
            underKeyGap=values.get("underkeyGap");
            xSliderValue=values.get("shiftXValue");
            ySliderValue=values.get("shiftYValue");
            extraLever=values.get("extraLever");
            prowFactor=values.get("prowFactor");

            periodSteps= (int) Math.round(values.get("halfStepsToPeriod"));
            generatorSteps=(int) Math.round(values.get("halfStepsToGenerator"));
            desiredGamut=(int) Math.round(values.get("gamut"));
            range=(int) Math.round(values.get("range"));
            startingKey=(int) Math.round(values.get("startingKey"));
            stepsForLarge=(int) Math.round(values.get("halfStepsToLargeMOSStep"));

            shiftXTrue = (int) Math.round(values.get("shiftXtrue")) == 1;
            roughRender = (int) Math.round(values.get("roughRender")) == 1;
            return true;

        } catch (FileNotFoundException | NumberFormatException ex) {
            //config file does not exist- return false to load default constants
            System.out.println("preset file does not exist or corrupted. Default constants loaded");
            return false;
        }

    }

    public boolean setConstantsFromLists(Map<String, Double> doubleValues, Map<String, Integer> intValues) {

        try{
            octaveWidth=doubleValues.get("octaveWidth");
            blackKeyHeight=doubleValues.get("blackKeyHeight");
            blackKeyLength=doubleValues.get("blackKeyLength");
            whiteKeyLengthPreShortening=doubleValues.get("whiteKeyLength");
            keytopHeightDifference=doubleValues.get("keytopHeightDiff");
            metalRoundRadius=doubleValues.get("metalRoundRadius");
            xToleranceGap=doubleValues.get("stalkFitXTolerance");
            yToleranceGap=doubleValues.get("stalkFitYTolerance");
            keyScale=doubleValues.get("keytopScale");
            underKeyGap=doubleValues.get("underkeyGap");
            xSliderValue=doubleValues.get("shiftXValue");
            ySliderValue=doubleValues.get("shiftYValue");
            extraLever=doubleValues.get("extraLever");
            prowFactor=doubleValues.get("prowFactor");


            periodSteps= Math.round(intValues.get("halfStepsToPeriod"));
            generatorSteps=Math.round(intValues.get("halfStepsToGenerator"));
            desiredGamut= Math.round(intValues.get("gamut"));
            range= Math.round(intValues.get("range"));
            startingKey= Math.round(intValues.get("startingKey"));
            stepsForLarge= Math.round(intValues.get("halfStepsToLargeMOSStep"));

            shiftXTrue =  Math.round(intValues.get("shiftXtrue")) == 1;
            roughRender =  Math.round(intValues.get("roughRender")) == 1;
            verticalFlip =  Math.round(intValues.get("verticalFlip")) == 1;
            manualKeyFlip = verticalFlip;
            return true;

        } catch (NumberFormatException ex) {

            return false;
        }

    }


    public void setDefaultConstants(){
        octaveWidth = 164;
        blackKeyHeight = 13;
        blackKeyLength = 100;
        whiteKeyLengthPreShortening = 148;

        keytopHeightDifference = 15;
        metalRoundRadius = 2.5;
        prowFactor = 0.5;
        extraLever = 0.0;

        periodSteps = 12;
        generatorSteps = 5;
        stepsForLarge = 1;
        desiredGamut = 24;//2*periodSteps?
        range = 12;
        startingKey = 5;

        //Gaps for stalkHole fit
        xToleranceGap = 0.3;
        yToleranceGap = 0.2;

        keyScale=0.875;

        underKeyGap = 0.46875;

        shiftXTrue = true;//if not, shift Y. This is terrible variable naming
        roughRender = false;
        
        verticalFlip = false;
        manualKeyFlip = verticalFlip;

        xSliderValue=0.25;
        ySliderValue=0.25;

        renderPath="C:\\Users\\JLMor\\Desktop\\OPENSCAD_DUMP";
    }

    public void getUserInputAndDeriveConstants() {
        keytopsInSingleKeyFiles = false;
        keytopsInTogether = true;

        periodWidth = octaveWidth / 12 * periodSteps;
        underKeyWidth = octaveWidth / 12.0 - underKeyGap;
        genhPreShortening = whiteKeyLengthPreShortening / desiredGamut;
        whiteKeyHeight = blackKeyHeight + metalRoundRadius + Math.sqrt(metalRoundRadius * metalRoundRadius * 2) + 4;

        determineMOS();

        System.out.println("Coprime? " +checkCoprime(periodSteps, generatorSteps));

        int chosenMosScaleIndex;

        for (int i = 0; i < mosTracker.size(); i++) {
            if (mosTracker.get(i).largeSize == stepsForLarge) {
                chosenMosScaleIndex = i;
                stepsForSmall = mosTracker.get(chosenMosScaleIndex).smallSize;
                break;
            }
        }

        determineGens();//find out what generator values get you to large and small step, as well as 1 step in whole tuning

            a = genForSmallStep * genhPreShortening;
            b = stepsForSmall / (periodSteps) * periodWidth;
            c = stepsForLarge / (periodSteps) * periodWidth;
            d = genForLargeStep * genhPreShortening;

        /*
        -Calculate a b c and d as if we didn't have to worry about not putting the keytops directly over the pivot point
        -Makes the white key shorter (by overhead, and 0.25*(a+d) so that not even the top of the highest key is over the pivot point)
        -Recalculate a b c and d for this new length.
        */

        whiteKeyLength = whiteKeyLengthPreShortening - (metalRoundRadius * 2 + 4 + (a + d) * 0.25);//dang haha I think the + 4 thing should be a variable. It's... the extra distance for the metal round/rod hole from the edge I think
        genh = whiteKeyLength / desiredGamut;

            a = genForSmallStep * genh;
            b = stepsForSmall / (periodSteps * 1.0) * periodWidth;
            c = stepsForLarge / (periodSteps * 1.0) * periodWidth;
            d = genForLargeStep * genh;
            
            System.out.println("verticalFlip: " +verticalFlip);
            System.out.println("neededAbsoluteValue: " +neededAbsoluteValue);
            
            if(manualKeyFlip)
            {
                System.out.println("inside vertical flip slash needed absolute value");
                double tempA, tempB;
                tempA = a;
                tempB = b;
                a=d;
                b=c;
                d=tempA;
                c=tempB;
            }
            
        System.out.println("A:" +a
                            +"\nB:" +b
                            +"\nC:" +c
                            +"\nD:" +d +"");


        shiftX = (b + c)/4*xSliderValue;
        shiftY = (a + d)/4*ySliderValue;
        
        holeScaleY = 0.5;

        if (shiftXTrue) {
            slantCutWidth = b + c - shiftX;
        } else {
            slantCutWidth = (b + c) / 2 - underKeyWidth / 6;
        }
            holeScaleX = 0.5;//Math.min(underKeyWidth / ((b + c)), 0.5 * keyScale);
            stalkScaleX = holeScaleX;//(holeScaleX*(b+c)-xToleranceGap)/(holeScaleX*(b+c))*holeScaleX;//Math.min(underKeyWidth-xToleranceGap / ((b + c)), ((b+c)*keyScale*holeScaleX-xToleranceGap)/((b+c)*keyScale*holeScaleX)*holeScaleX*keyScale);
            stalkScaleY = holeScaleY;//(holeScaleY*(a+d-shiftY*2)-yToleranceGap)/(holeScaleY*(a+d-shiftY*2))*holeScaleY;//((a+d - shiftY*2)*keyScale*holeScaleY-yToleranceGap)/((a+d - shiftY*2)*keyScale*holeScaleY)*holeScaleY;
        
        overhead = metalRoundRadius * 2 + 4 + (a + d) * 0.33;
    }

    public void generateFiles() {
        int currentPianoKey, currentGenerator;

        File together = new File(this.renderPath+"\\together.scad");//together is the big collection of keys and keytops that will show if everything worked correctly
        together.getParentFile().mkdirs();
        try {
            togetherPrint = new PrintWriter(together, "UTF-8");
        } catch (Exception e) {
            System.out.println(e);
        }
        togetherPrint.println("include<values.scad>;");


        File values = new File(this.renderPath+"\\values.scad");
        values.getParentFile().mkdirs();
        try {
            pwValues = new PrintWriter(values, "UTF-8");
        } catch (Exception e) {
            System.out.println(e);
        }

        createValuesFile();
        pwValues.close();

        for (int i = 0; i < range; i++) {//iterate keys until range is reached.
            currentPianoKey = (i + startingKey) % 12;//started on starting key, have moved i times, keys 12 steps apart are same underlying note
            currentGenerator = (i * genForStep1) % periodSteps;
            
            //modulo periodSteps because they share the same topside of the key and I want to start at the lowest one so that I include it, because I only move upwards later one, I think
            int keytopsNeeded = (desiredGamut - currentGenerator - 1) / periodSteps + 1;//-1 then plus one because if desiredGamut-currentGenerator)=periodSteps, I want it to return 1?

            try {
                File file2 = new File(this.renderPath+"\\" + i + "_" + currentGenerator+ ".scad");
                file2.getParentFile().mkdirs();
                pw = new PrintWriter(file2, "UTF-8");

                togetherPrint.println("use<" + i + "_" + currentGenerator + ".scad>;");

                togetherPrint.println("translate([-" + (i * octaveWidth / 12) + ",0,0");

                togetherPrint.println("])");

                togetherPrint.println(i + "_" + currentGenerator + "(" + keytopsInTogether+ ");");

                pw.println("use<keytop.scad>");
                pw.println("include<values.scad>");
                pw.println(i + "_" + currentGenerator + "();");
                pw.println("module " + i + "_" + currentGenerator + "(keytops=" + keytopsInSingleKeyFiles + "){");

                createMainBase(currentGenerator, keytopsNeeded, i, currentPianoKey);
                createKeyStalks(currentGenerator, keytopsNeeded, currentPianoKey);
                pw.println("}");

                thinCuts(currentGenerator);

                pw.println("}\n}");

                pw.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        createKeytop();//after crazy for loop for bases, make keytops file ONCE OH YEAH ONCE

        togetherPrint.close();
    }

    public void createValuesFile() {
        if (roughRender) {
            pwValues.println("$fs=2;");
            pwValues.println("$fa=20;");
        } else {
            pwValues.println("$fs=0.3675;");
            pwValues.println("$fa=5;");
        }
        pwValues.println("//Constants:");
        pwValues.println("edgeRadius=" + edgeRadius + ";");
        pwValues.println("underKeyWidth=" + underKeyWidth + ";");
        pwValues.println("blackKeyHeight=" + blackKeyHeight + ";");
        pwValues.println("whiteKeyHeight=" + (blackKeyHeight + metalRoundRadius + Math.sqrt(metalRoundRadius * metalRoundRadius * 2) + 4) + ";");
        System.out.println("whiteKeyHeight: " + whiteKeyHeight + ";");
        pwValues.println("genh=" + genh + ";");
        pwValues.println("a=" + a + ";");
        pwValues.println("b=" + b + ";");
        pwValues.println("c=" + c + ";");
        pwValues.println("d=" + d + ";");
        pwValues.println("overhead=" + overhead + ";");
        pwValues.println("desiredGamut=" + desiredGamut + ";");
        pwValues.println("shiftX=" + shiftX + ";");
        pwValues.println("holeScaleX=" + holeScaleX + ";");
        pwValues.println("holeScaleY=" + holeScaleY + ";");
        pwValues.println("stalkScaleX=" + stalkScaleX + ";");
        pwValues.println("stalkScaleY=" + stalkScaleY + ";");
        pwValues.println("keyScale=" + keyScale + ";");
        pwValues.println("metalRoundRadius=" + metalRoundRadius + ";");
        pwValues.println("metalRoundRadiusTolerance=" + metalRoundRadiusTolerance + ";");
        pwValues.println("extraLever=" + extraLever + ";");
        pwValues.println("prowFactor=" + prowFactor + ";");
        pwValues.println("keytopHeight=" + keytopHeight + ";");
        pwValues.println("tolerance=" + tolerance + ";");
        pwValues.println("slantCutWidth=" + slantCutWidth + ";");
        pwValues.println("shiftY =" + shiftY + ";");
        pwValues.println("keytopHeightDifference =" + keytopHeightDifference + ";");
        pwValues.println("blackKeyLength =" + blackKeyLength + ";");
        pwValues.println("whiteKeyLengthPreShortening =" + whiteKeyLengthPreShortening + ";");


        if (shiftXTrue) {
            pwValues.println("");
            pwValues.println("Points = [");//shiftX widens the tips and squishes the middle to make it hexagonal and tall
            pwValues.println("[shiftX,a,0],//0");
            pwValues.println("[(b-shiftX),0,0],//1");
            pwValues.println("[(b+shiftX),0,0],//2");
            pwValues.println("[(b+c-shiftX),d,0],//3");
            pwValues.println("[(c+shiftX),(a+d),0],//4");
            pwValues.println("[(c-shiftX),(a+d),0],//5");

            pwValues.println("[shiftX,a,10],//6");
            pwValues.println("[(b-shiftX),0,keytopHeight],//7");
            pwValues.println("[(b+shiftX),0,keytopHeight],//8");
            pwValues.println("[(b+c-shiftX),d,keytopHeight],//9");
            pwValues.println("[(c+shiftX),(a+d),keytopHeight],//10");
            pwValues.println("[(c-shiftX),(a+d),keytopHeight],//11");
            pwValues.println("];");

        } else {
            pwValues.println("");
            pwValues.println("Points = [");//shiftY stretches the side points vertically hexagonal and short
            pwValues.println("[0,a+shiftY,0],//0");
            pwValues.println("[0,a-shiftY,0],//1");
            pwValues.println("[b,shiftY,0],//2");
            pwValues.println("[(b+c),d-shiftY,0],//3");
            pwValues.println("[(b+c),d+shiftY,0],//4");
            pwValues.println("[c,(a+d)-shiftY,0],//5");

            pwValues.println("[0,a+shiftY,keytopHeight],//6");
            pwValues.println("[0,a-shiftY,keytopHeight],//7");
            pwValues.println("[b,shiftY,keytopHeight],//8");
            pwValues.println("[(b+c),d-shiftY,keytopHeight],//9");
            pwValues.println("[(b+c),d+shiftY,keytopHeight],//10");
            pwValues.println("[c,(a+d)-shiftY,keytopHeight],//11");
            pwValues.println("];");
        }
            pwValues.println("Faces = [");
            pwValues.println("[0,1,2,3,4,5],");
            pwValues.println("[1,0,6,7],");
            pwValues.println("[2,1,7,8],");
            pwValues.println("[3,2,8,9],");
            pwValues.println("[4,3,9,10],");
            pwValues.println("[5,4,10,11],");
            pwValues.println("[0,5,11,6],");
            pwValues.println("[11,10,9,8,7,6] ");
            pwValues.println("];");//faces for polygon in counter clockwise points looking from the inside of the key
            pwValues.println("");
    }

    public void createMainBase(int currentGenerator, int keytopsNeeded, int i, int currentPianoKey) {
        double length;//length of main base
        if(verticalFlip)
        length = genh * (periodSteps - currentGenerator + (keytopsNeeded - 1) * periodSteps) + overhead + stalkScaleY * (a + d);
        else
        length = genh * (currentGenerator + (keytopsNeeded - 1) * periodSteps) + overhead + stalkScaleY * (a + d);
        
        System.out.println("length: " +length);
        pw.println("length=" + length +";");
        pw.println("difference(){"
                + "\nunion(){");
        pw.println("");
        pw.println("//Main Base:");

        if (!isWhiteKey(currentPianoKey)) {
            pw.println("translate([0,0,blackKeyHeight])");
        }
        //raise up black key for easier combination in togetherPrint

        pw.println("difference(){");
        pw.println("union(){");
        
        pw.println("translate([0,-extraLever,0])"); 

        if (isWhiteKey(currentPianoKey)) {//first little chunk to hold the metal round, independent of the rest of the main base
            pw.println("cube([underKeyWidth,metalRoundRadius*2+4+extraLever,whiteKeyHeight],false);");
        } else {
            pw.println("cube([underKeyWidth,metalRoundRadius*2+4+extraLever,metalRoundRadius+sqrt(metalRoundRadius*metalRoundRadius*2)+4],false);");
        }

        pw.println("translate([0,metalRoundRadius*2+4,0])");

        if (isWhiteKey(currentPianoKey)) {//main section, white key is taller
            pw.println("cube([underKeyWidth,length-(metalRoundRadius*2+4),whiteKeyHeight],false);");
        } else {
            pw.println("cube([underKeyWidth,length-(metalRoundRadius*2+4),metalRoundRadius+sqrt(metalRoundRadius*metalRoundRadius*2)+4],false);");
        }

        pw.println("translate([0.5*underKeyWidth,length,0])");
        if (isWhiteKey(currentPianoKey)) {//rounded tip
            pw.println("cylinder(h=whiteKeyHeight, r=underKeyWidth/6);");
        } else {
            pw.println("cylinder(h=metalRoundRadius+sqrt(metalRoundRadius*metalRoundRadius*2)+4, r=underKeyWidth/6);");
        }

        pw.println("}");

        if (isWhiteKey(currentPianoKey)) {//round backs of white keys, vertically
            pw.println("translate([0,-extraLever,0])"); 
            pw.println("difference(){"
                    + "translate([0,blackKeyHeight,0])\n"
                    + "rotate([45,0,0])\n"
                    + "translate([-25,-50,0])\n"
                    + "cube([50,50,50]);\n"
                    + "translate([0,blackKeyHeight*2,blackKeyHeight])\n"
                    + "rotate([0,90,0])\n"
                    + "cylinder(r=blackKeyHeight*2, h=underKeyWidth);\n"
                    + "}\n");
        }

        pw.println("translate([0,-extraLever,0])"); 
        pw.println("difference(){\n"
                + "translate([-tolerance,-tolerance,-tolerance])\n");
        if (!isWhiteKey(currentPianoKey)) {
            pw.println("cube([underKeyWidth+tolerance*2,underKeyWidth/6, whiteKeyHeight-blackKeyHeight+tolerance*2]);\n"
                    + "translate([underKeyWidth/6,underKeyWidth/6,0])\n"
                    + "cylinder(r=underKeyWidth/6, h=whiteKeyHeight-blackKeyHeight+3*tolerance);\n"
                    + "translate([underKeyWidth-underKeyWidth/6,underKeyWidth/6,0])\n"
                    + "cylinder(r=underKeyWidth/6, h=whiteKeyHeight-blackKeyHeight);\n"
                    + "translate([underKeyWidth/6,-tolerance,-tolerance])\n"
                    + "cube([underKeyWidth*2/3, underKeyWidth/6+tolerance, whiteKeyHeight-blackKeyHeight+tolerance]);\n");

        } else {
            pw.println("cube([underKeyWidth+tolerance*2,underKeyWidth/6, whiteKeyHeight+tolerance*2]);\n"
                    + "translate([underKeyWidth/6,underKeyWidth/6,0])\n"
                    + "cylinder(r=underKeyWidth/6, h=whiteKeyHeight+3*tolerance);\n"
                    + "translate([underKeyWidth*5/6,underKeyWidth/6,0])\n"
                    + "cylinder(r=underKeyWidth/6, h=whiteKeyHeight+3*tolerance);\n"
                    + "translate([underKeyWidth/6,-tolerance,-tolerance])\n"
                    + "cube([underKeyWidth*2/3, underKeyWidth/6+tolerance, whiteKeyHeight+tolerance]);\n");
        }

        pw.println("}\n");
        pw.println("translate([0,-extraLever,0])"); 
        if (isWhiteKey(currentPianoKey)) {
            pw.println("translate([-.1,metalRoundRadius+2,blackKeyHeight+metalRoundRadius+2])");
        } else {
            pw.println("translate([-.1,metalRoundRadius+2,metalRoundRadius+2])");
        }

        pw.println("union(){"//round rod hole + square top?
                + "rotate([45,0,0])"
                + "cube([underKeyWidth+0.2,metalRoundRadius+metalRoundRadiusTolerance,metalRoundRadius+metalRoundRadiusTolerance]);"
                + "rotate([0,90,0])");
        //cylinder transform and rotate for metal round, 0.1 to make the extra 0.2 stick out and not be flush with base
        pw.println("cylinder((underKeyWidth+.2),r=metalRoundRadius+metalRoundRadiusTolerance, true);}");//cylinder for metal round, CHANGED FROM +0.5 TO +0.25

        pw.println("translate([0,-extraLever,0])");
        if (isWhiteKey(currentPianoKey)) {
            pw.println("translate([underKeyWidth/10,-tolerance,0.125*blackKeyHeight+blackKeyHeight])");
        } else {
            pw.println("translate([underKeyWidth/10,-tolerance,0.125*(metalRoundRadius*2+8)])");//+8 because of 4 around hole I think I dunno who cares
        }

        pw.println("mirror([0,1,0])");//key number label
        pw.println("rotate([90,0,0])");
        pw.println("linear_extrude(height=0.5){");
        pw.println("text(\"" + i + "\",size=underKeyWidth/2);");//(metalRoundRadius*2+4)*0.75)//PROBALBY WANT TO FIGURE OUT HOW TO CENTER TEXT VERTICALLY AND HORIZONTALLY
        pw.println("}");

        if (isWhiteKey(currentPianoKey)) {
            pw.println("translate([-0.5,0,-0.1])"
                    + "rotate([90,0,90])"
                    + "linear_extrude(height=underKeyWidth+1){"
                    + "\npolygon(points=["
                    + "\n[whiteKeyLengthPreShortening,0],"
                    + "\n[length+underKeyWidth*1/6,0],"
                    + "\n[length+underKeyWidth*1/6, whiteKeyHeight*prowFactor]"
                    + "\n]);"
                    + "\n}");
        } else {
            pw.println("translate([-0.5,0,-0.1])"
                    + "rotate([90,0,90])"
                    + "linear_extrude(height=underKeyWidth+1){"
                    + "\npolygon(points=["
                    + "\n[blackKeyLength-extraLever,0],"
                    + "\n[length+underKeyWidth*1/6,0],"
                    + "\n[length+underKeyWidth*1/6, blackKeyHeight*prowFactor]"
                    + "\n]);"
                    + "\n}");
        }

        pw.println("//Warp Cuts:");

        if (isWhiteKey(currentPianoKey)) {
            pw.println("warpHeight=" + whiteKeyHeight + ";");
            warpCuts(whiteKeyHeight, length, whiteKeyLengthPreShortening, true);
        } else {
            pw.println("warpHeight=" + (whiteKeyHeight - blackKeyHeight) + ";");
            warpCuts(whiteKeyHeight - blackKeyHeight, length, blackKeyLength, false);
        }

        pw.println("}");

    }

    //chekc white or black key, stop after under length
    public void warpCuts(double warpHeight, double underlyingLength, double length, boolean whiteKey) {
        for (double soFar = metalRoundRadius * 2 + 4; soFar < length - 0.5 * warpHeight && soFar < underlyingLength - 0.5 * warpHeight; soFar += warpHeight) {
            pw.println("translate([0,0.25*warpHeight,0])");
            pw.println("translate([-1," + soFar + "," + "warpHeight*0.5])");
            pw.println("rotate([0,90,0])");
            pw.println("linear_extrude(height=underKeyWidth+2)");
            pw.println("polygon(points=[[0,0],[0.25*warpHeight,0.125*warpHeight],[0.25*warpHeight,-0.125*warpHeight]]);");
            if (soFar < length - 1.25 * warpHeight && soFar < underlyingLength - 0.76 * warpHeight) {
                pw.println("translate([0,0.25*warpHeight,0])");
                pw.println("translate([-1," + soFar + "+warpHeight*0.5," + "0.75*warpHeight])");
                pw.println("rotate([0,90,0])");
                pw.println("linear_extrude(height=underKeyWidth+2)");
                pw.println("polygon(points=[[0,0],[0.25*warpHeight,0.125*warpHeight],[0.25*warpHeight,-0.125*warpHeight]]);");
            }
        }
    }

    public void thinCuts(int currentGenerator) {
        pw.println("//Thin Cuts:");
        pw.println("translate([0,0,-tolerance]){");
        pw.println("linear_extrude(height=whiteKeyHeight+2*tolerance)");
        pw.println("polygon(points=[[-0.1,10],[-0.1,length+0.1],[underKeyWidth/3,length+0.1]]);");
        pw.println("linear_extrude(height=whiteKeyHeight+2*tolerance)");
        pw.println("polygon(points=[[underKeyWidth+0.1,10],[underKeyWidth+0.1,length+0.1],[underKeyWidth/3*2,length+0.1]]);");
        pw.println("\n}");

        pw.println("anglePoints=[\n"
                + "[-underKeyWidth/3,10,whiteKeyHeight],\n"
                + "[0,10,whiteKeyHeight],\n"
                + "[underKeyWidth/3,length+0.1,whiteKeyHeight],\n"
                + "[0,length+0.1,whiteKeyHeight],\n"
                + "[-slantCutWidth-underKeyWidth/3,10,whiteKeyHeight+slantCutWidth],\n"
                + "[-slantCutWidth,10,whiteKeyHeight+slantCutWidth],\n"
                + "[-slantCutWidth+underKeyWidth/3,length+0.1,whiteKeyHeight+slantCutWidth],\n"
                + "[-slantCutWidth,length+0.1,whiteKeyHeight+slantCutWidth]];");

        pw.println("angleFaces=["
                + "[0,1,2,3],"
                + "[0,4,5,1],"
                + "[0,3,7,4],"
                + "[3,2,6,7],"
                + "[1,5,6,2],"
                + "[7,6,5,4]"
                + "];");

        pw.println("translate([0,0,0])polyhedron(anglePoints,angleFaces);");

        pw.println("translate([underKeyWidth,0,0])mirror([1,0,0])polyhedron(anglePoints,angleFaces);");


    }

    public void createKeyStalks(int currentGeneratorIn, int keytopsNeeded, int currentPianoKeyIn) {//in order for flipping to work, I need to change where the keytops/keystalks are rendered, front to back
        pw.println("//Key Stalks:");
        for (int j = 0; j < keytopsNeeded; j++) {//j changes generator to enharmonically eqauivalent values by being increasing by periodSteps until greater than gamut
            
            if(verticalFlip){
                if (isWhiteKey(currentPianoKeyIn)) {
                    pw.println("translate([underKeyWidth/3,genh*" + (desiredGamut - currentGeneratorIn) + "+overhead,0.75*(whiteKeyHeight)]){");
                } else {
                    pw.println("translate([underKeyWidth/3,genh*" + (desiredGamut - currentGeneratorIn) + "+overhead,blackKeyHeight+0.75*(metalRoundRadius+sqrt(metalRoundRadius*metalRoundRadius*2)+4)]){");
                }
            }
            else{
                if (isWhiteKey(currentPianoKeyIn)) {
                    pw.println("translate([underKeyWidth/3,genh*" + currentGeneratorIn + "+overhead,0.75*(whiteKeyHeight)]){");
                } else {
                    pw.println("translate([underKeyWidth/3,genh*" + currentGeneratorIn + "+overhead,blackKeyHeight+0.75*(metalRoundRadius+sqrt(metalRoundRadius*metalRoundRadius*2)+4)]){");
            }
            }
            
            
            if (isWhiteKey(currentPianoKeyIn)) {
                pw.println("linear_extrude(height=(keytopHeight+keytopHeightDifference-(" + ((double) (currentGeneratorIn + 1.0) / (double) desiredGamut) * keytopHeightDifference + ")+0.25*(whiteKeyHeight))){");//*0.75 to leave 0.25 extra to be taken up by support angle things
            } else {
                pw.println("linear_extrude(height=(keytopHeight+keytopHeightDifference-(" + ((double) (currentGeneratorIn + 1.0) / (double) desiredGamut) * keytopHeightDifference + ")+0.25*(metalRoundRadius+sqrt(metalRoundRadius*metalRoundRadius*2)+4))){");
            }

            pw.println("scale([keyScale,keyScale,1])");
            pw.println("polygon(points=[");
            pw.println("[0,0],[underKeyWidth/3,0],[underKeyWidth/3,(a+d)/2],[0,(a+d)/2]");
            pw.println("]);");
            pw.println("}");

            pw.println("if(keytops)");
            pw.println("translate([-0.25*(b+c),(-0.25*(a+d)),60-" + ((double) currentGeneratorIn / (double) desiredGamut) * keytopHeightDifference);
            if (!isWhiteKey(currentPianoKeyIn)) {
                pw.println("-((blackKeyHeight+0.75*(metalRoundRadius+sqrt(metalRoundRadius*metalRoundRadius*2)+4))-0.75*(whiteKeyHeight))");//if black key, drop down by the difference bewteen the different stalks' starting heights
            }
            pw.println("])");//
            pw.println("keytop();");
            pw.println("}");
            currentGeneratorIn += periodSteps;
        }
    }

    public void createKeytop() {
        try {
            File file = new File(renderPath+"\\keytop.scad");
            file.getParentFile().mkdirs();
            pwKeyTop = new PrintWriter(file, "UTF-8");

            pwKeyTop.println("include<values.scad>;");
            
            pwKeyTop.println("keytop();");
            pwKeyTop.println("");

            pwKeyTop.println("module keytopShape(){");//
            pwKeyTop.println("polyhedron(Points,Faces);");
            pwKeyTop.println("}");//

            pwKeyTop.println("module keytop(){");//make it a module so that together.scad can use it
            pwKeyTop.println("scale([keyScale,keyScale,1]){");
            pwKeyTop.println("difference(){");
            pwKeyTop.println("keytopShape();");//make key
            
            
            pwKeyTop.println("translate([1/2*(b+c)-1/6*underKeyWidth,1/2*(d+a)-(1/2*(d+a)*holeScaleY),-0.01])");
            //pwKeyTop.println("scale([holeScaleX,holeScaleY,0.5])");
            pwKeyTop.println("linear_extrude(height = 0.5*keytopHeight)");
            pwKeyTop.println("polygon(points=[");
            pwKeyTop.println("[0,0],[underKeyWidth/3+"+xToleranceGap+",0],[underKeyWidth/3+"+xToleranceGap+",(a+d)/2+"+yToleranceGap+"],[0,(a+d)/2+"+yToleranceGap+"]");
            pwKeyTop.println("]);");
            //pwKeyTop.println("keytopShape();");
            
            //edges rounded by subtracting module below
            if (shiftXTrue) {
                pwKeyTop.print("edge(11,6,1,");
                pwKeyTop.println("-1);");

                pwKeyTop.print("edge(6,7,1,");
                if (b<2*shiftX || c<2*shiftX) pwKeyTop.print("-");
                pwKeyTop.println("1);");

                pwKeyTop.print("edge(7,8,1,");
                pwKeyTop.println("1);");

                pwKeyTop.print("edge(8,9,1,");
                pwKeyTop.println("1);");

                pwKeyTop.print("edge(9,10,1,");
                if (b<2*shiftX || c<2*shiftX) pwKeyTop.print("-");
                pwKeyTop.println("-1);");

                pwKeyTop.print("edge(10,11,1,");
                pwKeyTop.println("-1);");
            } else {
                pwKeyTop.print("edge(11,6,1,");
                pwKeyTop.println("-1);");

                pwKeyTop.print("edge(6,7,1,");
                pwKeyTop.println("1);");

                pwKeyTop.print("edge(7,8,1,");
                pwKeyTop.println("1);");

                pwKeyTop.print("edge(8,9,1,");
                pwKeyTop.println("1);");

                pwKeyTop.print("edge(9,10,1,");
                pwKeyTop.println("1);");

                pwKeyTop.print("edge(10,11,1,");
                if (a<2*shiftY || d<2*shiftY) pwKeyTop.print("-");
                pwKeyTop.println("-1);");
            }

            pwKeyTop.println("}");
            pwKeyTop.println("}");

            pwKeyTop.println("module edge (point1,point2,d1,d2){");//annoying combination of cylinders and cubes to round edges
            pwKeyTop.println("difference(){");
            pwKeyTop.println("translate([(Points[point1][0]+Points[point2][0])/2,(Points[point1][1]+Points[point2][1])/2,(Points[point1][2]+Points[point2][2])/2])");
            pwKeyTop.println("rotate([0,90,atan((Points[point2][1]-Points[point1][1])/(Points[point2][0]-Points[point1][0]))])");
            pwKeyTop.println("rotate([0,0,45])");
            pwKeyTop.println("cube([pow(2*(edgeRadius*edgeRadius),1/2),pow(2*(edgeRadius*edgeRadius),1/2),pow(pow(Points[point1][0]-Points[point2][0],2)+pow(Points[point1][1]-Points[point2][1],2),0.5)*2],true);");

            pwKeyTop.println("translate([(Points[point1][0]+Points[point2][0])/2,(Points[point1][1]+Points[point2][1])/2,(Points[point1][2]+Points[point2][2])/2])");
            pwKeyTop.println("rotate([0,90,atan((Points[point2][1]-Points[point1][1])/(Points[point2][0]-Points[point1][0]))])");
            pwKeyTop.println("translate([d1*edgeRadius,d2*edgeRadius,0])");
            pwKeyTop.println("cylinder(pow(pow(Points[point1][0]-Points[point2][0],2)+pow(Points[point1][1]-Points[point2][1],2),0.5)*2,edgeRadius+tolerance,edgeRadius+tolerance, true);");//don't need to fill in much here since it's already got the points
            pwKeyTop.println("}");
            pwKeyTop.println("}");
            pwKeyTop.println("}");//DIFFERENCE END
            pwKeyTop.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public boolean isWhiteKey(int currentPianoKey) {
        return currentPianoKey == 0
                ||//0 is A, if key is a white key...
                currentPianoKey == 2
                || currentPianoKey == 3
                || currentPianoKey == 5
                || currentPianoKey == 7
                || currentPianoKey == 8
                || currentPianoKey == 10;
    }

    public void determineMOS() {
        ArrayList<Integer> scaleSteps = new ArrayList<>();

        StepSizeTracker stepSizes;

        scaleSteps.add(0);
        scaleSteps.add(periodSteps);

        mosSizes.add(1);
        mosSizes.add(2);
        mosSizes.add(3);

        int numberLess = 1;
        //steps.add(periodSteps);
        for (int currentGenerator = 1; currentGenerator < periodSteps; currentGenerator++) {
            stepSizes = new StepSizeTracker();
            scaleSteps.add((currentGenerator * generatorSteps) % periodSteps);
            Collections.sort(scaleSteps);
            //HashMap<Integer, Integer> tempStepHash;

            for (int h = 1; h < scaleSteps.size() - numberLess; h++) {//need to keep track of h in stepSizes to check for how many times it shows up
                //tempStepHash = new HashMap<>();
                for (int i = h; i < scaleSteps.size() + h - numberLess; i++) {//works so far first time through where h=1 and size is 3
                    if (i > scaleSteps.size() - 1) {
                        stepSizes.addStep(h, scaleSteps.get(i - (scaleSteps.size() - 1)) + periodSteps - scaleSteps.get(i - h));
                    } else {
                        stepSizes.addStep(h, scaleSteps.get(i) - scaleSteps.get(i - h));
                    }
                }
            }

            boolean isMOS = true;
            for (int i = 0; i < stepSizes.stepBags.size(); i++) {
                if (stepSizes.stepBags.get(i).size() > 2) {
                    isMOS = false;
                }
            }

            if (isMOS && isValidMOS(scaleSteps.size() - 1, mosSizes)) {
                mosSizes.add(scaleSteps.size() - 1);
                if (stepSizes.stepBags.get(0).size() == 2) {
                    mosTracker.add(new mosScale(
                            scaleSteps.size() - 1,
                            stepSizes.stepBags.get(0).get(0).size,
                            stepSizes.stepBags.get(0).get(0).amount,
                            stepSizes.stepBags.get(0).get(1).size,
                            stepSizes.stepBags.get(0).get(1).amount));
                } else {
                    mosTracker.add(new mosScale(
                            scaleSteps.size() - 1,
                            stepSizes.stepBags.get(0).get(0).size,
                            stepSizes.stepBags.get(0).get(0).amount));
                }
            }
        }
    }

    public boolean isValidMOS(int test, ArrayList<Integer> mosSizes) {
        for (int i = 0; i < mosSizes.size(); i++) {
            for (int j = i; j < mosSizes.size(); j++) {
                if (test == mosSizes.get(j) + mosSizes.get(i)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void determineGens() {//iterate through stacked generators until you arrive at large step, use to derive small step

        boolean foundLargeGen = false, foundSmallGen = false, foundGenForStep1 = false;//maybe it's the foundGenForStep1 that I fucked shit up to work around
        int currentStepsAboveTonic = generatorSteps;//starting at one generator, so starting at the number of generator steps in the tuning
        int generatorCounter = 1;

        while ((foundLargeGen && foundSmallGen && foundGenForStep1) == false) {

            while (currentStepsAboveTonic > periodSteps - 1) {
                currentStepsAboveTonic -= periodSteps;
            }

            if (currentStepsAboveTonic == stepsForLarge) {
                foundLargeGen = true;
                genForLargeStep = Math.min(generatorCounter, Math.abs(periodSteps - generatorCounter));//if you went up more than half, the other generator is faster there... going up. I think.
                
                if((generatorCounter)>Math.abs(periodSteps - generatorCounter)){
                    neededAbsoluteValue=true;//should change the variable name to "used alternate generator" or something
                    System.out.println("inside first neededAbsoluteValue");
                }
                //And I'm just counting numbers of generators away? which is weird.
            }

            if (currentStepsAboveTonic == stepsForSmall) {
                foundSmallGen = true;
                genForSmallStep = Math.min(generatorCounter, Math.abs(periodSteps - generatorCounter));
            }

            if (currentStepsAboveTonic == 1) {
                foundGenForStep1 = true;
                genForStep1 = Math.min(generatorCounter, Math.abs(periodSteps - generatorCounter));
                genForStep1b = Math.abs(genForStep1 - periodSteps);
            }

            if (generatorCounter > periodSteps) {
                System.out.println("I DON'T THINK THAT THOSE TWO NUMBERS ARE COPRIME!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                break;
            }

            currentStepsAboveTonic += generatorSteps;
            generatorCounter++;
        }
        if (stepsForLarge == 1) {
            genForLargeStep = Math.abs(genForSmallStep - periodSteps);
            neededAbsoluteValue = false;//!neededAbsoluteValue;//////////////////////////////////////////////////////////////////////////////////////////////woah why am I touching this here? just to flip the result? eek, apparently that's about right maybe
            System.out.println("inside stepsForLarge==1, neededAbsoluteValue");
        }
    }

    public boolean checkCoprime(int per, int gen) {//make sure user input of generator generates entire gamut of periodSteps
        int current = gen;
        boolean areCoprime = true;//
        for (int i = 0; i < per - 1; i++) {
            if (current == per) {
                areCoprime = false;
            }
            current += gen;
            if (current > per) {
                current -= per;
            }
        }
        return areCoprime;
    }

    public double getA() {
        return a;
    }

    public double getB() {
        return b;
    }

    public double getC() {
        return c;
    }

    public double getD() {
        return d;
    }
}

class mosScale {
    int scaleSize;
    int smallSteps;
    int smallSize;
    int largeSteps;
    int largeSize;

    public mosScale(int scaleSizeIn, int size1In, int steps1In, int size2In, int steps2In) {
        scaleSize = scaleSizeIn;
        if (size1In < size2In) {
            smallSteps = steps1In;
            smallSize = size1In;
            largeSteps = steps2In;
            largeSize = size2In;
        } else {
            smallSteps = steps2In;
            smallSize = size2In;
            largeSteps = steps1In;
            largeSize = size1In;
        }
    }

    public mosScale(int scaleSizeIn, int smallSizeIn, int smallStepsIn) {//I might have made this one for when there is only one step size? It might be getting me into/out of trouble, I don't remember
        scaleSize = scaleSizeIn;
        smallSteps = smallStepsIn;
        smallSize = smallSizeIn;
        largeSteps = smallStepsIn - scaleSize;
        largeSize = smallSize;
    }
}

class StepSizeTracker {
    ArrayList<ArrayList<StepBag>> stepBags;
    boolean alreadyHere;

    StepSizeTracker() {
        stepBags = new ArrayList<>();
    }

    void addStep(Integer intervalClass, Integer stepSize) {
        if (stepBags.size() < intervalClass) {
            stepBags.add(new ArrayList<StepBag>());
        }
        alreadyHere = false;
        for (int i = 0; i < stepBags.get(intervalClass - 1).size(); i++) {
            if (stepSize == stepBags.get(intervalClass - 1).get(i).size) {
                stepBags.get(intervalClass - 1).get(i).amount++;
                alreadyHere = true;
            }
        }
        if (alreadyHere == false) {
            stepBags.get(intervalClass - 1).add(new StepBag(stepSize, 1));
        }
    }
}

class StepBag {
    int size;
    int amount;

    StepBag(int sizeIn, int amountIn) {
        size = sizeIn;
        amount = amountIn;
    }
}