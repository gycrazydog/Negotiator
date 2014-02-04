package group3;

import java.util.HashMap;
import negotiator.boaframework.AcceptanceStrategy;
import negotiator.boaframework.Actions;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OfferingStrategy;

/**
 * Adapt mixed strategies, choose acceptance strategy by looking at the time remains.
 * Three classic AC conditions are implemented here, including ACnext,ACtime and ACconst
 * 
 * TeamWork: Canran Gou and Shijie Li
 * 
 * @author Shijie Li
 */
public class Group3_AS extends AcceptanceStrategy {
	
	private double a;
	private double b;
    private double time;
	private double lambda = .0,lU;//Variables for calculating reserved utility during ACtime 
	private double ACnextT = 0.9;//Deadline for ACnext strategy
	private double ACtimeU = 0.8;//reserve utility during ACtime
	private double ACconsta = 0.6;//reserve utility in the end of session
	private double greatBidUtil = 0.95;
	public Group3_AS() { }
	
	public Group3_AS(NegotiationSession negoSession, OfferingStrategy strat, double alpha, double beta){
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;
		this.a =  alpha;
		this.b = beta;
	}

	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy strat, HashMap<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;

		if (parameters.get("a") != null || parameters.get("b") != null) {
			a = parameters.get("a");
			b = parameters.get("b");
		} else {
			a = 1;
			b = 0;
		}
	}
	
	@Override
	public String printParameters() {
		String str = "[a: " + a + " b: " + b + "]";
		return str;
	}

	@Override
	public Actions determineAcceptability() {
		//Get next bid
		double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
		//Get last opponent bid
		double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
		time = negotiationSession.getTime();
		//If opponent propose a bid with really high utility,which in most cases more than 0.95,accept it.
		if(lastOpponentBidUtil > greatBidUtil)
			return Actions.Accept;
		//When it's early in session, implement ACnext,if last opponent bid is not better than next own bid, reject it.
		if(time<ACnextT){
			if(lastOpponentBidUtil> nextMyBidUtil){
				return Actions.Accept;
			}
			else
				return Actions.Reject;
		}
		else{
			//Calculating average bidding time for a round(me and opponent),and check if it's possible to make another round.
			double totalsessions = (double)negotiationSession.getOpponentBidHistory().size()+(double)negotiationSession.getOwnBidHistory().size();
			double avgtime;
			avgtime = time/totalsessions;
			/*
			 * If rest time is less than the average time, it seems impossible to do another round, which is dangerous.
			 * In case in the end of session agents take more take to calculate, I relaxed the deadline for ACtime a little bit.
			 */
			if(1-time<avgtime*10)
			{
				if(lastOpponentBidUtil>ACconsta)
				return Actions.Accept;
				else
				return Actions.Reject;
			}
			/*
			 * If it's still possible to make a bit, calculate the alpha for ACconst by taking time into account.
			 * When it's getting later, the lU(lowUtility) will decrease more sharply.
			 */
			else
			{
				setLambda(avgtime);
				lU = getlU(time);
				//opponent bid is better than alpha of ACconst
				if(lastOpponentBidUtil>lU)
				return Actions.Accept;
				else
				return Actions.Reject;
			}
		}
	}
    
	//Regression function for ACconst with time.
    private double getlU(double time) {
		return 2-Math.pow(Math.E, lambda*(time-ACnextT));
	}

	public void setLambda(double avgtime) {
    	lambda = Math.log(2-ACtimeU)/(1-ACnextT-avgtime);
    }
        
    
}