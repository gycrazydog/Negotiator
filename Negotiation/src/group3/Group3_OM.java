package group3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import negotiator.Bid;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OpponentModel;
import negotiator.boaframework.offeringstrategy.anac2010.IAMhaggler2010.EvaluatorHypothesis;
import negotiator.issue.Issue;
import negotiator.issue.Objective;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;
import negotiator.issue.IssueDiscrete;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.UtilitySpace;

/**
 * Implement scalable bayesian learning algorithm from paper "Opponent Modelling in Automated Multi-Issue Negotiation 
 * Using Bayesian Learning" -- Koen Hindriks and Dmytro Tykhonov
 * 
 * TeamWork: Canran Gou and Shijie Li
 * 
 * @author Canran Gou
 */
public class Group3_OM extends OpponentModel {
	private int amountOfIssues;
	private double[] weights;
	private ArrayList<UtilitySpace> hypow;
	/*
	 * 	utility space of each hypothesis for issues and evaluations
	 *	For example the ith item of piw Arraylist is the HashMap linking the utilitySpace of ith issue and the Double is 
	 *	probability of the issue being weighted in utilitySpace. So is the case for pie.
	*/
	private ArrayList<HashMap<UtilitySpace,Double>> piw;
	private ArrayList<HashMap<UtilitySpace,Double>> pie;
	//expected values for issues and evaluations
	private double[] ExpWUtility;
	private double[] ExpEUtility;
	@Override
	public void init(NegotiationSession negotiationSession, HashMap<String, Double> parameters) throws Exception {
		this.negotiationSession = negotiationSession;
		initializeModel();
	}
	
	private void initializeModel(){
		opponentUtilitySpace = new UtilitySpace(negotiationSession.getUtilitySpace());
		amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		//initialize bayesian components,including the utility space for each hypothesis of issues and evaluations
		initializeBayesianComponents();
	}
	
	private void initializeBayesianComponents() {
		this.weights = new double[amountOfIssues+1];
		double aoi = (double)amountOfIssues;
		//enumerate the hypothesis of weights for issues
		for(int i = 1 ; i <= amountOfIssues ; i ++)
		{
			weights[i] = 2*(i)/(aoi*(aoi+1));
		}
		//initialize all data structures for utilitySpaces
		HashMap<Objective,Evaluator> hypos = new HashMap<Objective,Evaluator>();
		piw = new ArrayList<HashMap<UtilitySpace,Double>>();
		pie = new ArrayList<HashMap<UtilitySpace,Double>>();
		for(int i = 0 ; i <= amountOfIssues ; i ++)
		{
			piw.add(new HashMap<UtilitySpace,Double>());
			pie.add(new HashMap<UtilitySpace,Double>());
		}
		hypow = new ArrayList<UtilitySpace>();
		ExpWUtility = new double[amountOfIssues+1];
		ExpEUtility = new double[amountOfIssues+1];
		
		Set<Entry<Objective, Evaluator>>opspace = opponentUtilitySpace.getEvaluators();
		try {
			/*
			 * 	Construct the initial probability for each issue and evaluation,
			 *	Because here we implement scalable algorithm, so we store and learn pw and pe separately.
			 */
			constructHypoIssue(1,opspace,hypos);
			constructHypoEvaluator(opspace);
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*
		 * During constructing pw, each time we find an issue, we put it into the Arraylist hypow,
		 * and then here we initialize all P(hijw) for all issues under all hypothesis
		 */
		int size = hypow.size();
		for(int j = 1 ; j <= amountOfIssues ;j++)
		{	
			HashMap<UtilitySpace,Double> tempmap= piw.get(j);
			for(int i = 0 ; i < size ; i ++)
				{
					tempmap.put(hypow.get(i), 1.0/(double)(hypow.size()));
				}
			piw.set(j, tempmap);
		}
	}

	private void constructHypoEvaluator(Set<Entry<Objective, Evaluator>> opspace) throws Exception {
		//For each issue of outcomespace, we iterate all possible values and initialize probabilities.
		for(Entry<Objective, Evaluator> entry : opspace)
		{
			IssueDiscrete id = ((IssueDiscrete)entry.getKey());
			EvaluatorDiscrete eva = (EvaluatorDiscrete)entry.getValue();
			int issuenum = id.getNumber();
			int num = id.getNumberOfValues();
			HashMap<UtilitySpace,Double> tempmap = pie.get(issuenum);
			eva.setWeight(1.0/amountOfIssues);
			for(ValueDiscrete vd : id.getValues())
			{	
				UtilitySpace newspace = new UtilitySpace(negotiationSession.getDomain());
				//Assume vd is the top in the shape of evaluator function
				eva.setEvaluation(vd,100);
				//Then the values for all other xi can be calculated with assumption in paper
				for(ValueDiscrete vd1 : id.getValues())
				{
					if(vd1==vd)
					continue;
					else if(id.getValueIndex(vd1)<id.getValueIndex(vd))
						eva.setEvaluation(vd1, id.getValueIndex(vd1)*100/id.getValueIndex(vd));
					else
						eva.setEvaluation(vd1, (id.getValueIndex(vd1)-num)*100/(id.getValueIndex(vd)-num));
				}
				//For each value of a certain issue, the initial possibility is uniformly distributed
				newspace.addEvaluator(id, eva);
				tempmap.put(newspace, 1.0/(double)num);
			}
			//When finishing construction for an issue, set Hashmap to the correspondent position in Arraylist
			pie.set(issuenum, tempmap);
		}
	}

	/*
	 * Call this method recursively to construct hypothesis for possible weights for issues.
	 * Basic idea is confirm a rank firstly, then assign the weight of this rank to every issue, then call next rank.
	 */
	private void constructHypoIssue(int rank,Set<Entry<Objective, Evaluator>>opspace,HashMap<Objective,Evaluator> hypos) throws Exception {
		if(rank>amountOfIssues){
			/*
			 * finish constructing one hypothesis of issue weights, which are store in hypos.
			 * Push it into Arraylist for further constructing the probability sets.
			 */
			UtilitySpace newspace = new UtilitySpace(negotiationSession.getDomain());
			for(Entry<Objective, Evaluator> entry : hypos.entrySet()){
				newspace.addEvaluator(entry.getKey(), entry.getValue());
				newspace.unlock(entry.getKey());
			}
			hypow.add(newspace);
			return ;
		}
		//set weights of this rank to issues, store the weight and issue in Hashmap, then go into next rank level.
		for(Entry<Objective, Evaluator> entry : opspace)
		{
			if(hypos.containsKey(entry.getKey())) continue;
			IssueDiscrete id = ((IssueDiscrete)entry.getKey());
			EvaluatorDiscrete eva = (EvaluatorDiscrete)entry.getValue();
			eva.setWeight(weights[rank]);
			for(ValueDiscrete vd : id.getValues()){
				eva.setEvaluation(vd,1);
			}
			hypos.put(entry.getKey(), eva);
			constructHypoIssue(rank+1,opspace,hypos);	
			hypos.remove(entry.getKey());
		}
		return;
	}

	/**
	 * Updates the opponent model given a bid.
	 */
	@Override
	public void updateModel(Bid opponentBid, double time) {		
		try{
			double sum = 0.0;
			/*
			 * Calculate current expected hw and he for every issue with given opponentBid.
			 * Then calculate u(bt) for opponentBid by multiply he and hw for every issue.
			 */
			for(int k = 1 ; k <= amountOfIssues ; k ++){
				ExpEUtility[k] = 0.0;
				ExpWUtility[k] = 0.0;
				HashMap<UtilitySpace,Double> curpe = pie.get(k);
				for(Entry<UtilitySpace,Double> temp : curpe.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
					ExpEUtility[k] += val*hh.getEvaluation(k, opponentBid);
				}
				
				HashMap<UtilitySpace,Double> curpw = piw.get(k);
				for(Entry<UtilitySpace,Double> temp : curpw.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
					ExpWUtility[k] += val*hh.getWeight(k);
				}
				//Implement u(bt) += hiw*hie(bt)
				sum += ExpWUtility[k]*ExpEUtility[k];
			}
			for(int k = 1 ; k <= amountOfIssues ; k ++){
				double sume = 0.0;
				double sumw = 0.0;
				//Iterate all hypothesis for issue weights and calculate p(he|bt)
				HashMap<UtilitySpace,Double> curpe = pie.get(k);
				for(Entry<UtilitySpace,Double> temp : curpe.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
					//calculating p(bt) by adding p(bt|hkje)
					sume += val*condiProb((sum-ExpEUtility[k]*ExpWUtility[k])+ExpWUtility[k]*hh.getEvaluation(k, opponentBid));
				}
				for(Entry<UtilitySpace,Double> temp : curpe.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
					//update p(hkje|bt)
					val = val*condiProb((sum-ExpEUtility[k]*ExpWUtility[k])+ExpWUtility[k]*hh.getEvaluation(k, opponentBid))/sume;
					curpe.put(hh,val);
				}
				//Iterate all hypothesis for issue weights and calculate p(hw|bt)
				HashMap<UtilitySpace,Double> curpw = piw.get(k);
				for(Entry<UtilitySpace,Double> temp : curpw.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
					//calculating p(bt) by adding p(bt|hkjw)
					sumw += val*condiProb((sum-ExpEUtility[k]*ExpWUtility[k])+ExpWUtility[k]*val*hh.getWeight(k));
				}
				for(Entry<UtilitySpace,Double> temp : curpw.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
					//update p(hkjw|bt)
					val = val*condiProb((sum-ExpEUtility[k]*ExpWUtility[k])+ExpWUtility[k]*val*hh.getWeight(k))/sumw;
					curpw.put(hh,val);
				}
				pie.set(k, curpe);		
				piw.set(k, curpw);
			}
		}
		catch(Exception e){
			System.out.println("Opps!");
		}
	}

	//calculate the p(bt|hj), in scalable case, p(U<-k>(bt)+hkwj*hkw|hkwj)
	private Double condiProb(double utility) throws Exception {
		double thrta = 0.1;
		double utilityExp = 1-0.05*negotiationSession.getTime();
		double expoArg = (-1)*(utility-utilityExp)*(utility-utilityExp)/(2*thrta*thrta);
		double ans = Math.pow(Math.E, expoArg)/(thrta*Math.sqrt(2*Math.PI));
		return ans;
	}

	@Override
	public double getBidEvaluation(Bid bid) {
		double result = 0;
		try {
			//calculate the u(bt) by multiple expected hw and he for every issue
			for(int k = 1 ; k <= amountOfIssues ; k ++){
				double uE = 0.0;
				double uW = 0.0;
				
				//calculate the expected pke
				HashMap<UtilitySpace,Double> curpe = pie.get(k);
				for(Entry<UtilitySpace,Double> temp : curpe.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
					uE += val*hh.getEvaluation(k, bid);
				}
				//calculate the expected pkw
				HashMap<UtilitySpace,Double> curpw = piw.get(k);
				for(Entry<UtilitySpace,Double> temp : curpw.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
					uW += val*hh.getWeight(k);
				}
				//calculate u(bt)
				result += uE*uW;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	@Override
	public String getName() {
		return "Group3_OpponentModeling";
	}
}