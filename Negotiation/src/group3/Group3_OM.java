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
 * BOA framework implementation of the HardHeaded Frequecy Model.
 * My main contribution to this model is that I fixed a bug in the mainbranch
 * which resulted in an equal preference of each bid in the ANAC 2011 competition.
 * Effectively, the corrupt model resulted in the offering of a random bid in the ANAC 2011.
 * 
 * Default: learning coef l = 0.2; learnValueAddition v = 1.0
 * 
 * Adapted by Mark Hendrikx to be compatible with the BOA framework.
 *
 * Tim Baarslag, Koen Hindriks, Mark Hendrikx, Alex Dirkzwager and Catholijn M. Jonker.
 * Decoupling Negotiating Agents to Explore the Space of Negotiation Strategies
 * 
 * @author Mark Hendrikx
 */
public class Group3_OM extends OpponentModel {
	private int amountOfIssues;
	private double[] weights;
	private ArrayList<UtilitySpace> hypow;
	private ArrayList<HashMap<UtilitySpace,Double>> piw;
	private ArrayList<HashMap<UtilitySpace,Double>> pie;
	private double[] ExpWUtility;
	private double[] ExpEUtility;
	/**
	 * Initializes the utility space of the opponent such that all value
	 * issue weights are equal.
	 */
	@Override
	public void init(NegotiationSession negotiationSession, HashMap<String, Double> parameters) throws Exception {
		this.negotiationSession = negotiationSession;
		initializeModel();
	}
	
	private void initializeModel(){
		opponentUtilitySpace = new UtilitySpace(negotiationSession.getUtilitySpace());
		amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		
		initializeWeight();
	}
	
	private void initializeWeight() {
		// TODO Auto-generated method stub
		this.weights = new double[amountOfIssues+1];
		double aoi = (double)amountOfIssues;
		for(int i = 1 ; i <= amountOfIssues ; i ++)
		{
			weights[i] = 2*(i)/(aoi*(aoi+1));
		}
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
			constructHypoIssue(1,opspace,hypos);
			constructHypoEvaluator(opspace);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		List<BidDetails> os = negotiationSession.getOutcomeSpace().getAllOutcomes();
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
		// TODO Auto-generated method stub
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
				eva.setEvaluation(vd,100);
				for(ValueDiscrete vd1 : id.getValues())
				{
					if(vd1==vd)
					continue;
					else if(id.getValueIndex(vd1)<id.getValueIndex(vd))
						eva.setEvaluation(vd1, id.getValueIndex(vd1)*100/id.getValueIndex(vd));
					else
						eva.setEvaluation(vd1, (id.getValueIndex(vd1)-num)*100/(id.getValueIndex(vd)-num));
				}
				newspace.addEvaluator(id, eva);
				tempmap.put(newspace, 1.0/(double)num);
			}
			pie.set(issuenum, tempmap);
		}
	}

	private void constructHypoIssue(int rank,Set<Entry<Objective, Evaluator>>opspace,HashMap<Objective,Evaluator> hypos) throws Exception {
		if(rank>amountOfIssues){
			UtilitySpace newspace = new UtilitySpace(negotiationSession.getDomain());
			for(Entry<Objective, Evaluator> entry : hypos.entrySet()){
				newspace.addEvaluator(entry.getKey(), entry.getValue());
				newspace.unlock(entry.getKey());
			}
			hypow.add(newspace);
			return ;
		}
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
				sum += ExpWUtility[k]*ExpEUtility[k];
			}
			for(int k = 1 ; k <= amountOfIssues ; k ++){
				double sume = 0.0;
				double sumw = 0.0;
				//calculate  p(he|bt)
				HashMap<UtilitySpace,Double> curpe = pie.get(k);
				for(Entry<UtilitySpace,Double> temp : curpe.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
					sume += val*condiProb((sum-ExpEUtility[k]*ExpWUtility[k])+ExpWUtility[k]*hh.getEvaluation(k, opponentBid));
				}
				for(Entry<UtilitySpace,Double> temp : curpe.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
					val = val*condiProb((sum-ExpEUtility[k]*ExpWUtility[k])+ExpWUtility[k]*hh.getEvaluation(k, opponentBid))/sume;
					curpe.put(hh,val);
				}
				//calculate p(hw|bt)
				HashMap<UtilitySpace,Double> curpw = piw.get(k);
				for(Entry<UtilitySpace,Double> temp : curpw.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
					sumw += val*condiProb((sum-ExpEUtility[k]*ExpWUtility[k])+ExpWUtility[k]*val*hh.getWeight(k));
				}
				for(Entry<UtilitySpace,Double> temp : curpw.entrySet()){
					UtilitySpace hh = (UtilitySpace)temp.getKey();
					Double val = (double)temp.getValue();
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