package br.otimizes.oplatool.core.jmetal4.experiments.base;

import br.otimizes.oplatool.common.exceptions.JMException;
import br.otimizes.oplatool.core.jmetal4.core.OPLASolution2;
import br.otimizes.oplatool.core.jmetal4.database.Result;
import br.otimizes.oplatool.core.jmetal4.experiments.EdCalculation;
import br.otimizes.oplatool.core.jmetal4.operators.crossover.PLACrossoverOperator2;
import br.otimizes.oplatool.core.jmetal4.operators.mutation.PLAMutationOperator2;
import br.otimizes.oplatool.core.jmetal4.problems.OPLA;
import br.otimizes.oplatool.core.jmetal4.problems.OPLA2;
import br.otimizes.oplatool.core.persistence.ExperimentConfs;
import br.otimizes.oplatool.core.persistence.Persistence;
import br.otimizes.oplatool.domain.OPLAThreadScope;
import br.otimizes.oplatool.domain.entity.Experiment;
import br.ufpr.dinf.gres.loglog.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.component.ranking.Ranking;
import org.uma.jmetal.component.ranking.impl.MergeNonDominatedSortRanking;
import org.uma.jmetal.component.termination.Termination;
import org.uma.jmetal.component.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.MultithreadedSolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

import java.io.File;
import java.util.HashMap;
import java.util.List;

@Service
public class NSGAIIOPLABase2 implements AlgorithmBase<NSGAIIConfigs> {

    private static final Logger LOGGER = Logger.getLogger(NSGAIIOPLABase2.class);

    private final Persistence mp;
    private final EdCalculation c;

    public NSGAIIOPLABase2(Persistence mp, EdCalculation c) {
        this.mp = mp;
        this.c = c;
    }

    private static String getPlaName(String pla) {
        int beginIndex = pla.lastIndexOf(File.separator) + 1;
        int endIndex = pla.length() - 4;
        return pla.substring(beginIndex, endIndex);
    }

    public void execute(NSGAIIConfigs configs) throws Exception {
        Experiment experiment;
        String[] plas = configs.getPlas().split(",");
        String xmiFilePath;

        for (String pla : plas) {
            xmiFilePath = pla;
            OPLA2 problem = null;
            String plaName = getPlaName(pla);
            try {
                problem = new OPLA2(xmiFilePath, configs);
            } catch (Exception e) {
                e.printStackTrace();
                configs.getLogger()
                        .putLog(String.format("Error when try read architecture %s. %s", xmiFilePath, e.getMessage()));
                throw new JMException("Ocorreu um erro durante geração de PLAs");
            }
            Result result = new Result();
            experiment = mp.save(plaName, "NSGAII", configs.getDescription(), OPLAThreadScope.hashOnPosteriori.get());
            ExperimentConfs conf = new ExperimentConfs(experiment.getId(), "NSGAII", configs);
            mp.save(conf);


            PLACrossoverOperator2 crossover = new PLACrossoverOperator2(configs.getCrossoverProbability());
            PLAMutationOperator2 mutation = new PLAMutationOperator2(configs.getMutationProbability());

            int populationSize = 100;
            int offspringPopulationSize = populationSize;

            Termination termination = new TerminationByEvaluations(configs.getMaxEvaluations());

            Ranking<OPLASolution2> ranking = new MergeNonDominatedSortRanking<>();
            BinaryTournamentSelection<OPLASolution2> selection = new BinaryTournamentSelection<OPLASolution2>();

            SolutionListEvaluator<OPLASolution2> evaluator = new MultithreadedSolutionListEvaluator<>(8) ;
            NSGAII<OPLASolution2> algorithm = new NSGAIIBuilder<>(problem, crossover, mutation, populationSize)
                    .setSelectionOperator(selection)
                    .setMaxEvaluations(configs.getMaxEvaluations())
                    .setSolutionListEvaluator(evaluator)
                    .build();

            algorithm.run();
            List<OPLASolution2> solutions = algorithm.getResult();
            System.out.println(solutions);

            evaluator.shutdown();

//            https://jmetal.readthedocs.io/en/latest/mnds.html
        }
    }

    private void logInformations(String pla, NSGAIIConfigs configs, int populationSize, int maxEvaluations, double crossoverProbability, double mutationProbability) {
        logPanel(pla, configs, populationSize, maxEvaluations, crossoverProbability, mutationProbability);
        logConsole(pla, populationSize, maxEvaluations, crossoverProbability, mutationProbability);
    }

    private void logPanel(String pla, NSGAIIConfigs configs, int populationSize, int maxEvaluations, double crossoverProbability, double mutationProbability) {
        configs.getLogger().putLog("\n================ NSGAII ================", Level.INFO);
        configs.getLogger().putLog("Context: OPLA", Level.INFO);
        configs.getLogger().putLog("PLA: " + pla, Level.INFO);
        configs.getLogger().putLog("Params:", Level.INFO);
        configs.getLogger().putLog("\tPop -> " + populationSize, Level.INFO);
        configs.getLogger().putLog("\tMaxEva -> " + maxEvaluations, Level.INFO);
        configs.getLogger().putLog("\tCross -> " + crossoverProbability, Level.INFO);
        configs.getLogger().putLog("\tMuta -> " + mutationProbability, Level.INFO);
        long heapSize = Runtime.getRuntime().totalMemory();
        heapSize = (heapSize / 1024) / 1024;
        configs.getLogger().putLog("Heap Size: " + heapSize + "Mb\n");
    }

    private void logConsole(String pla, int populationSize, int maxEvaluations, double crossoverProbability, double mutationProbability) {
        LOGGER.info("================ NSGAII ================");
        LOGGER.info("Context: OPLA");
        LOGGER.info("PLA: " + pla);
        LOGGER.info("Params:");
        LOGGER.info("tPop -> " + populationSize);
        LOGGER.info("tMaxEva -> " + maxEvaluations);
        LOGGER.info("tCross -> " + crossoverProbability);
        LOGGER.info("tMuta -> " + mutationProbability);
        LOGGER.info("================ NSGAII ================");
    }

}
