//  Experiment.java
//
//  Authors:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Antonio J. Nebro, Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package br.otimizes.oplatool.core.jmetal4.experiments.util;

import br.otimizes.oplatool.core.jmetal4.core.Algorithm;
import br.otimizes.oplatool.core.jmetal4.experiments.settings.Settings;
import br.otimizes.oplatool.common.exceptions.JMException;
import br.otimizes.oplatool.domain.config.FileConstants;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class representing jMetal experiments
 */
public abstract class Experiment {

    public String experimentName_;
    public String[] algorithmNameList_;
    public String[] problemList_;
    public String[] paretoFrontFile_;
    public String[] indicatorList_;
    public String experimentBaseDirectory_;
    public String latexDirectory_;
    public String paretoFrontDirectory_;
    public String outputParetoFrontFile_;
    public String outputParetoSetFile_;
    public int independentRuns_;
    public Settings[] algorithmSettings_;
    public HashMap<String, Boolean> indicatorMinimize_;
    public Properties[] problemsSettings_;
    HashMap<String, Object> map_;

    public Experiment() {
        experimentName_ = "noName";

        problemsSettings_ = null;

        map_ = new HashMap<String, Object>();

        algorithmNameList_ = null;
        problemList_ = null;
        paretoFrontFile_ = null;
        indicatorList_ = null;

        experimentBaseDirectory_ = "";
        paretoFrontDirectory_ = "";
        latexDirectory_ = "latex";
        outputParetoFrontFile_ = "FUN";
        outputParetoSetFile_ = "VAR";
        algorithmSettings_ = null;
        independentRuns_ = 0;
        indicatorMinimize_ = new HashMap<String, Boolean>();
        indicatorMinimize_.put("HV", false);
        indicatorMinimize_.put("EPSILON", true);
        indicatorMinimize_.put("SPREAD", true);
        indicatorMinimize_.put("GD", true);
        indicatorMinimize_.put("IGD", true);
    } // Constructor

    public void runExperiment(int numberOfThreads) {
        checkExperimentDirectory();
        map_.put("experimentDirectory", experimentBaseDirectory_);
        map_.put("algorithmNameList", algorithmNameList_);
        map_.put("problemList", problemList_);
        map_.put("indicatorList", indicatorList_);
        map_.put("paretoFrontDirectory", paretoFrontDirectory_);
        map_.put("paretoFrontFile", paretoFrontFile_);
        map_.put("independentRuns", independentRuns_);
        map_.put("outputParetoFrontFile", outputParetoFrontFile_);
        map_.put("outputParetoSetFile", outputParetoSetFile_);
        map_.put("problemsSettings", problemsSettings_);
        if (problemList_.length < numberOfThreads) {
            numberOfThreads = problemList_.length;
            System.out
                    .println("Experiments: list of problems is shorter than the "
                            + "of requested threads. Creating "
                            + numberOfThreads);
        } // if
        else {
            System.out.println("Experiments: creating " + numberOfThreads
                    + " threads");
        }

        Thread[] p = new RunExperiment[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            p[i] = new RunExperiment(this, map_, i, numberOfThreads,
                    problemList_.length);
            p[i].start();
        }

        try {
            for (int i = 0; i < numberOfThreads; i++) {
                p[i].join();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(Experiment.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }

    public void runExperiment() throws JMException, IOException {
        runExperiment(1);
    } // runExperiment

    public void checkExperimentDirectory() {
        File experimentDirectory;
        experimentDirectory = new File(experimentBaseDirectory_);
        if (experimentDirectory.exists()) {
            System.out.println("Experiment directory exists");
            if (experimentDirectory.isDirectory()) {
                System.out.println("Experiment directory is a directory");
            } else {
                System.out
                        .println("Experiment directory is not a directory. Deleting file and creating directory");
            }
            experimentDirectory.delete();
            new File(experimentBaseDirectory_).mkdirs();
        } // if
        else {
            System.out.println("Experiment directory does NOT exist. Creating");
            new File(experimentBaseDirectory_).mkdirs();
        } // else
    } // checkDirectories

    public abstract void algorithmSettings(String problemName, int problemId,
                                           Algorithm[] algorithm) throws ClassNotFoundException;

    public void generateLatexTables() throws IOException {
        latexDirectory_ = experimentBaseDirectory_ + FileConstants.FILE_SEPARATOR + latexDirectory_;
        Vector[][][] data = new Vector[indicatorList_.length][][];
        for (int indicator = 0; indicator < indicatorList_.length; indicator++) {
            data[indicator] = new Vector[problemList_.length][];

            for (int problem = 0; problem < problemList_.length; problem++) {
                data[indicator][problem] = new Vector[algorithmNameList_.length];

                for (int algorithm = 0; algorithm < algorithmNameList_.length; algorithm++) {
                    data[indicator][problem][algorithm] = new Vector<Object>();

                    String directory = experimentBaseDirectory_;
                    directory += FileConstants.FILE_SEPARATOR + "data" + FileConstants.FILE_SEPARATOR;
                    directory += FileConstants.FILE_SEPARATOR + algorithmNameList_[algorithm];
                    directory += FileConstants.FILE_SEPARATOR + problemList_[problem];
                    directory += FileConstants.FILE_SEPARATOR + indicatorList_[indicator];
                    FileInputStream fis = new FileInputStream(directory);
                    InputStreamReader isr = new InputStreamReader(fis);
                    BufferedReader br = new BufferedReader(isr);
                    System.out.println(directory);
                    String aux = br.readLine();
                    while (aux != null) {
                        data[indicator][problem][algorithm].add(Double.parseDouble(aux));
                        System.out.println(Double.parseDouble(aux));
                        aux = br.readLine();
                    } // while
                    br.close();
                } // for
            } // for
        } // for

        double[][][] mean;
        double[][][] median;
        double[][][] stdDeviation;
        double[][][] iqr;
        double[][][] max;
        double[][][] min;
        int[][][] numberOfValues;

        Map<String, Double> statValues = new HashMap<String, Double>();

        statValues.put("mean", 0.0);
        statValues.put("median", 0.0);
        statValues.put("stdDeviation", 0.0);
        statValues.put("iqr", 0.0);
        statValues.put("max", 0.0);
        statValues.put("min", 0.0);

        mean = new double[indicatorList_.length][][];
        median = new double[indicatorList_.length][][];
        stdDeviation = new double[indicatorList_.length][][];
        iqr = new double[indicatorList_.length][][];
        min = new double[indicatorList_.length][][];
        max = new double[indicatorList_.length][][];
        numberOfValues = new int[indicatorList_.length][][];

        for (int indicator = 0; indicator < indicatorList_.length; indicator++) {
            mean[indicator] = new double[problemList_.length][];
            median[indicator] = new double[problemList_.length][];
            stdDeviation[indicator] = new double[problemList_.length][];
            iqr[indicator] = new double[problemList_.length][];
            min[indicator] = new double[problemList_.length][];
            max[indicator] = new double[problemList_.length][];
            numberOfValues[indicator] = new int[problemList_.length][];

            for (int problem = 0; problem < problemList_.length; problem++) {
                mean[indicator][problem] = new double[algorithmNameList_.length];
                median[indicator][problem] = new double[algorithmNameList_.length];
                stdDeviation[indicator][problem] = new double[algorithmNameList_.length];
                iqr[indicator][problem] = new double[algorithmNameList_.length];
                min[indicator][problem] = new double[algorithmNameList_.length];
                max[indicator][problem] = new double[algorithmNameList_.length];
                numberOfValues[indicator][problem] = new int[algorithmNameList_.length];

                for (int algorithm = 0; algorithm < algorithmNameList_.length; algorithm++) {
                    Collections.sort(data[indicator][problem][algorithm]);

                    String directory = experimentBaseDirectory_;
                    directory += FileConstants.FILE_SEPARATOR + algorithmNameList_[algorithm];
                    directory += FileConstants.FILE_SEPARATOR + problemList_[problem];
                    directory += FileConstants.FILE_SEPARATOR + indicatorList_[indicator];
                    calculateStatistics(data[indicator][problem][algorithm],
                            statValues);
                    mean[indicator][problem][algorithm] = statValues
                            .get("mean");
                    median[indicator][problem][algorithm] = statValues
                            .get("median");
                    stdDeviation[indicator][problem][algorithm] = statValues
                            .get("stdDeviation");
                    iqr[indicator][problem][algorithm] = statValues.get("iqr");
                    min[indicator][problem][algorithm] = statValues.get("min");
                    max[indicator][problem][algorithm] = statValues.get("max");
                    numberOfValues[indicator][problem][algorithm] = data[indicator][problem][algorithm]
                            .size();
                }
            }
        }

        File latexOutput;
        latexOutput = new File(latexDirectory_);
        if (!latexOutput.exists()) {
            System.out.println("Creating " + latexDirectory_ + " directory");
        }
        System.out.println("Experiment name: " + experimentName_);
        String latexFile = latexDirectory_ + FileConstants.FILE_SEPARATOR + experimentName_ + ".tex";
        printHeaderLatexCommands(latexFile);
        for (int i = 0; i < indicatorList_.length; i++) {
            printMeanStdDev(latexFile, i, mean, stdDeviation);
            printMedianIQR(latexFile, i, median, iqr);
        } // for
        printEndLatexCommands(latexFile);
    } // generateLatexTables

    public void calculateStatistics(Vector vector, Map<String, Double> values) {

        if (vector.size() > 0) {
            double sum, sqsum, min, max, median, mean, stdDeviation;

            sqsum = 0.0;
            sum = 0.0;
            min = 1E300;
            max = -1E300;
            median = 0;

            for (int i = 0; i < vector.size(); i++) {
                double val = (Double) vector.elementAt(i);

                sqsum += val * val;
                sum += val;
                if (val < min) {
                    min = val;
                }
                if (val > max) {
                    max = val;
                } // if
            } // for

            // Mean
            mean = sum / vector.size();

            // Standard deviation
            if (sqsum / vector.size() - mean * mean < 0.0) {
                stdDeviation = 0.0;
            } else {
                stdDeviation = Math.sqrt(sqsum / vector.size() - mean * mean);
            } // if

            // Median
            if (vector.size() % 2 != 0) {
                median = (Double) vector.elementAt(vector.size() / 2);
            } else {
                median = ((Double) vector.elementAt(vector.size() / 2 - 1) + (Double) vector
                        .elementAt(vector.size() / 2)) / 2.0;
            } // if

            values.put("mean", mean);
            values.put("median",
                    Statistics.calculateMedian(vector, 0, vector.size() - 1));
            values.put("iqr", Statistics.calculateIQR(vector));
            values.put("stdDeviation", stdDeviation);
            values.put("min", min);
            values.put("max", max);
        } // if
        else {
            values.put("mean", Double.NaN);
            values.put("median", Double.NaN);
            values.put("iqr", Double.NaN);
            values.put("stdDeviation", Double.NaN);
            values.put("min", Double.NaN);
            values.put("max", Double.NaN);
        } // else
    } // calculateStatistics

    public void printHeaderLatexCommands(String fileName) throws IOException {
        FileWriter os = new FileWriter(fileName, false);
        os.write("\\documentclass{article}" + "\n");
        os.write("\\title{" + experimentName_ + "}" + "\n");
        os.write("\\usepackage{colortbl}" + "\n");
        os.write("\\usepackage[table*]{xcolor}" + "\n");
        os.write("\\xdefinecolor{gray95}{gray}{0.65}" + "\n");
        os.write("\\xdefinecolor{gray25}{gray}{0.8}" + "\n");
        os.write("\\author{}" + "\n");
        os.write("\\begin{document}" + "\n");
        os.write("\\maketitle" + "\n");
        os.write("\\section{Tables}" + "\n");
        os.close();
    }

    public void printEndLatexCommands(String fileName) throws IOException {
        FileWriter os = new FileWriter(fileName, true);
        os.write("\\end{document}" + "\n");
        os.close();
    } // printEndLatexCommands

    public void printMeanStdDev(String fileName, int indicator, double[][][] mean,
                                double[][][] stdDev) throws IOException {
        FileWriter os = new FileWriter(fileName, true);
        os.write("\\" + "\n");
        os.write("\\begin{table}" + "\n");
        os.write("\\caption{" + indicatorList_[indicator]
                + ". Mean and standard deviation}" + "\n");
        os.write("\\label{table:mean." + indicatorList_[indicator] + "}" + "\n");
        os.write("\\centering" + "\n");
        os.write("\\begin{scriptsize}" + "\n");
        os.write("\\begin{tabular}{l");

        for (int i = 0; i < algorithmNameList_.length; i++) {
            os.write("l");
        }
        os.write("}\n");

        os.write("\\hline");
        for (int i = -1; i < algorithmNameList_.length; i++) {
            if (i == -1) {
                os.write(" & ");
            } else if (i == (algorithmNameList_.length - 1)) {
                os.write(" " + algorithmNameList_[i] + "\\\\" + "\n");
            } else {
                os.write("" + algorithmNameList_[i] + " & ");
            }
        }
        os.write("\\hline" + "\n");

        String m, s;
        for (int i = 0; i < problemList_.length; i++) {
            double bestValue;
            double bestValueIQR;
            double secondBestValue;
            double secondBestValueIQR;
            int bestIndex = -1;
            int secondBestIndex = -1;
            if (indicatorMinimize_.get(indicatorList_[indicator]) == true) {// minimize
                bestValue = Double.MAX_VALUE;
                bestValueIQR = Double.MAX_VALUE;
                secondBestValue = Double.MAX_VALUE;
                secondBestValueIQR = Double.MAX_VALUE;
                for (int j = 0; j < (algorithmNameList_.length); j++) {
                    if ((mean[indicator][i][j] < bestValue)
                            || ((mean[indicator][i][j] == bestValue) && (stdDev[indicator][i][j] < bestValueIQR))) {
                        secondBestIndex = bestIndex;
                        secondBestValue = bestValue;
                        secondBestValueIQR = bestValueIQR;
                        bestValue = mean[indicator][i][j];
                        bestValueIQR = stdDev[indicator][i][j];
                        bestIndex = j;
                    } else if ((mean[indicator][i][j] < secondBestValue)
                            || ((mean[indicator][i][j] == secondBestValue) && (stdDev[indicator][i][j] < secondBestValueIQR))) {
                        secondBestIndex = j;
                        secondBestValue = mean[indicator][i][j];
                        secondBestValueIQR = stdDev[indicator][i][j];
                    } // else if
                }
            } // if
            else { // indicator to maximize e.g., the HV
                bestValue = Double.MIN_VALUE;
                bestValueIQR = Double.MIN_VALUE;
                secondBestValue = Double.MIN_VALUE;
                secondBestValueIQR = Double.MIN_VALUE;
                for (int j = 0; j < (algorithmNameList_.length); j++) {
                    if ((mean[indicator][i][j] > bestValue)
                            || ((mean[indicator][i][j] == bestValue) && (stdDev[indicator][i][j] < bestValueIQR))) {
                        secondBestIndex = bestIndex;
                        secondBestValue = bestValue;
                        secondBestValueIQR = bestValueIQR;
                        bestValue = mean[indicator][i][j];
                        bestValueIQR = stdDev[indicator][i][j];
                        bestIndex = j;
                    } else if ((mean[indicator][i][j] > secondBestValue)
                            || ((mean[indicator][i][j] == secondBestValue) && (stdDev[indicator][i][j] < secondBestValueIQR))) {
                        secondBestIndex = j;
                        secondBestValue = mean[indicator][i][j];
                        secondBestValueIQR = stdDev[indicator][i][j];
                    } // else if
                } // for
            } // else

            os.write(problemList_[i] + " & ");
            for (int j = 0; j < (algorithmNameList_.length - 1); j++) {
                if (j == bestIndex) {
                    os.write("\\cellcolor{gray95}");
                }
                if (j == secondBestIndex) {
                    os.write("\\cellcolor{gray25}");
                }

                m = String.format(Locale.ENGLISH, "%10.2e",
                        mean[indicator][i][j]);
                s = String.format(Locale.ENGLISH, "%8.1e",
                        stdDev[indicator][i][j]);
                os.write("$" + m + "_{" + s + "}$ & ");
            }
            if (bestIndex == (algorithmNameList_.length - 1)) {
                os.write("\\cellcolor{gray95}");
            }
            m = String.format(Locale.ENGLISH, "%10.2e",
                    mean[indicator][i][algorithmNameList_.length - 1]);
            s = String.format(Locale.ENGLISH, "%8.1e",
                    stdDev[indicator][i][algorithmNameList_.length - 1]);
            os.write("$" + m + "_{" + s + "}$ \\\\" + "\n");
        } // for
        os.write("\\hline" + "\n");
        os.write("\\end{tabular}" + "\n");
        os.write("\\end{scriptsize}" + "\n");
        os.write("\\end{table}" + "\n");
        os.close();
    } // printMeanStdDev

    public void printMedianIQR(String fileName, int indicator, double[][][] median,
                               double[][][] IQR) throws IOException {
        FileWriter os = new FileWriter(fileName, true);
        os.write("\\" + "\n");
        os.write("\\begin{table}" + "\n");
        os.write("\\caption{" + indicatorList_[indicator] + ". Median and IQR}"
                + "\n");
        os.write("\\label{table:median." + indicatorList_[indicator] + "}"
                + "\n");
        os.write("\\begin{scriptsize}" + "\n");
        os.write("\\centering" + "\n");
        os.write("\\begin{tabular}{l");

        for (int i = 0; i < algorithmNameList_.length; i++) {
            os.write("l");
        }
        os.write("}\n");

        os.write("\\hline");
        for (int i = -1; i < algorithmNameList_.length; i++) {
            if (i == -1) {
                os.write(" & ");
            } else if (i == (algorithmNameList_.length - 1)) {
                os.write(" " + algorithmNameList_[i] + "\\\\" + "\n");
            } else {
                os.write("" + algorithmNameList_[i] + " & ");
            }
        }
        os.write("\\hline" + "\n");

        String m, s;
        for (int i = 0; i < problemList_.length; i++) {
            double bestValue;
            double bestValueIQR;
            double secondBestValue;
            double secondBestValueIQR;
            int bestIndex = -1;
            int secondBestIndex = -1;
            if (indicatorMinimize_.get(indicatorList_[indicator]) == true) {// minimize
                bestValue = Double.MAX_VALUE;
                bestValueIQR = Double.MAX_VALUE;
                secondBestValue = Double.MAX_VALUE;
                secondBestValueIQR = Double.MAX_VALUE;
                for (int j = 0; j < (algorithmNameList_.length); j++) {
                    if ((median[indicator][i][j] < bestValue)
                            || ((median[indicator][i][j] == bestValue) && (IQR[indicator][i][j] < bestValueIQR))) {
                        secondBestIndex = bestIndex;
                        secondBestValue = bestValue;
                        secondBestValueIQR = bestValueIQR;
                        bestValue = median[indicator][i][j];
                        bestValueIQR = IQR[indicator][i][j];
                        bestIndex = j;
                    } else if ((median[indicator][i][j] < secondBestValue)
                            || ((median[indicator][i][j] == secondBestValue) && (IQR[indicator][i][j] < secondBestValueIQR))) {
                        secondBestIndex = j;
                        secondBestValue = median[indicator][i][j];
                        secondBestValueIQR = IQR[indicator][i][j];
                    } // else if
                } // for
            } // if
            else { // indicator to maximize e.g., the HV
                bestValue = Double.MIN_VALUE;
                bestValueIQR = Double.MIN_VALUE;
                secondBestValue = Double.MIN_VALUE;
                secondBestValueIQR = Double.MIN_VALUE;
                for (int j = 0; j < (algorithmNameList_.length); j++) {
                    if ((median[indicator][i][j] > bestValue)
                            || ((median[indicator][i][j] == bestValue) && (IQR[indicator][i][j] < bestValueIQR))) {
                        secondBestIndex = bestIndex;
                        secondBestValue = bestValue;
                        secondBestValueIQR = bestValueIQR;
                        bestValue = median[indicator][i][j];
                        bestValueIQR = IQR[indicator][i][j];
                        bestIndex = j;
                    } else if ((median[indicator][i][j] > secondBestValue)
                            || ((median[indicator][i][j] == secondBestValue) && (IQR[indicator][i][j] < secondBestValueIQR))) {
                        secondBestIndex = j;
                        secondBestValue = median[indicator][i][j];
                        secondBestValueIQR = IQR[indicator][i][j];
                    } // else if
                } // for
            } // else

            os.write(problemList_[i] + " & ");
            for (int j = 0; j < (algorithmNameList_.length - 1); j++) {
                if (j == bestIndex) {
                    os.write("\\cellcolor{gray95}");
                }
                if (j == secondBestIndex) {
                    os.write("\\cellcolor{gray25}");
                }
                m = String.format(Locale.ENGLISH, "%10.2e",
                        median[indicator][i][j]);
                s = String
                        .format(Locale.ENGLISH, "%8.1e", IQR[indicator][i][j]);
                os.write("$" + m + "_{" + s + "}$ & ");
            }
            if (bestIndex == (algorithmNameList_.length - 1)) {
                os.write("\\cellcolor{gray95}");
            }
            m = String.format(Locale.ENGLISH, "%10.2e",
                    median[indicator][i][algorithmNameList_.length - 1]);
            s = String.format(Locale.ENGLISH, "%8.1e",
                    IQR[indicator][i][algorithmNameList_.length - 1]);
            os.write("$" + m + "_{" + s + "}$ \\\\" + "\n");
        } // for

        os.write("\\hline" + "\n");
        os.write("\\end{tabular}" + "\n");
        os.write("\\end{scriptsize}" + "\n");
        os.write("\\end{table}" + "\n");
        os.close();
    } // printMedianIQR

    public void generateRBoxplotScripts(int rows, int cols, String[] problems,
                                        String prefix, boolean notch, Experiment experiment)
            throws IOException {
        RBoxplot.generateScripts(rows, cols, problems, prefix, notch, this);
    } // generateRBoxplotScripts

    public void generateRWilcoxonScripts(String[] problems, String prefix,
                                         Experiment experiment) throws IOException {
        RWilcoxon.generateScripts(problems, prefix, this);
    } // generateRWilcoxonScripts
} // Experiment

