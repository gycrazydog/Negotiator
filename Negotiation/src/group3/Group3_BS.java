package group3;

import java.util.HashMap;

import negotiator.bidding.BidDetails;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OMStrategy;
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.boaframework.opponentmodel.NoModel;

/**
 * This is an abstract class used to implement a TimeDependentAgent Strategy adapted from [1]
 * 	[1]	S. Shaheen Fatima  Michael Wooldridge  Nicholas R. Jennings
 * 		Optimal Negotiation Strategies for Agents with Incomplete Information
 * 		http://eprints.ecs.soton.ac.uk/6151/1/atal01.pdf
 * 
 * The default strategy was extended to enable the usage of opponent models.
 * 
 * Note that this agent is not fully equivalent to the theoretical model, loading the domain
 * may take some time, which may lead to the agent skipping the first bid. A better implementation
 * is GeniusTimeDependent_Offering. 
 * 
 * @author Alex Dirkzwager, Mark Hendrikx
 */
public class Group3_BS extends OfferingStrategy {

	/** k \in [0, 1]. For k = 0 the agent starts with a bid of maximum utility */
	private double k;
	/** Maximum target utility */
	private double Pmax;
	/** Minimum target utility */
	private double Pmin;
	/** Concession factor */
	private double e;
	/** Outcome space */
	SortedOutcomeSpace outcomespace;
	double hardheadedT = 0.6;
	double reserveU = 0.7;
	double tradeoffU = 0.8;
	/**
	 * Empty constructor used for reflexion. Note this constructor assumes that init
	 * is called next.
	 */
	public Group3_BS(){}
	
	public Group3_BS(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, double e, double k, double max, double min){
		this.e = e;
		this.k = k;
		this.Pmax = max;
		this.Pmin = min;
		this.negotiationSession = negoSession;
		outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
		negotiationSession.setOutcomeSpace(outcomespace);
		this.opponentModel = model;
		this.omStrategy = oms;	
	}
	
	/**
	 * Method which initializes the agent by setting all parameters.
	 * The parameter "e" is the only parameter which is required.
	 */
	public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, HashMap<String, Double> parameters) throws Exception {
		if (parameters.get("e") != null) {
			this.negotiationSession = negoSession;
			
			outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
			negotiationSession.setOutcomeSpace(outcomespace);
			
			this.e = parameters.get("e");
			
			if (parameters.get("k") != null)
				this.k = parameters.get("k");
			else
				this.k = 0;
			
			if (parameters.get("min") != null)
				this.Pmin = parameters.get("min");
			else
				this.Pmin = negoSession.getMinBidinDomain().getMyUndiscountedUtil();
		
			if (parameters.get("max") != null) {
				Pmax= parameters.get("max");
			} else {
				BidDetails maxBid = negoSession.getMaxBidinDomain();
				Pmax = maxBid.getMyUndiscountedUtil();
			}
			
			this.opponentModel = model;
			this.omStrategy = oms;
		} else {
			throw new Exception("Constant \"e\" for the concession speed was not set.");
		}
	}

	@Override
	public BidDetails determineOpeningBid() {
		return determineNextBid();
	}

	//
	@Override
	public BidDetails determineNextBid() {
		double time = negotiationSession.getTime();
		BidDetails lastOpponentBid = negotiationSession.getOpponentBidHistory().getLastBidDetails();
		double utilityGoal;

		if(time<hardheadedT)
			return outcomespace.getMaxBidPossible();
		else{
			try{
				double bestUtil = tradeoffU;
				BidDetails bestBid = null;
				int issuenum = negotiationSession.getIssues().size();
				for(BidDetails bd : outcomespace.getAllOutcomes())
				{
					int diff = 0;
					for(int i = 1 ; i <= issuenum ; i ++ )
					{
						if(!bd.getBid().getValue(i).equals(lastOpponentBid.getBid().getValue(i)))
							diff++;
					}
					double curUtility = negotiationSession.getUtilitySpace().getUtility(bd.getBid());
					if(diff == 1&&curUtility>bestUtil)
					{
						bestUtil = curUtility;
						bestBid = bd;	
						System.out.println("bestBid  "+bestBid.getBid().toString()+" bestUtility " + bestUtil );
					}
				}
				if(bestBid != null)
					return bestBid;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			time = (time-hardheadedT)/(1-hardheadedT);
			utilityGoal = Pmax-(Pmax-reserveU)*time;

			if (opponentModel instanceof NoModel) {
				nextBid = negotiationSession.getOutcomeSpace().getBidNearUtility(utilityGoal);
			} else {
				nextBid = omStrategy.getBid(outcomespace.getAllOutcomes());
			try {
				double res = negotiationSession.getUtilitySpace().getUtility(nextBid.getBid());
				if(res<utilityGoal)
					nextBid = omStrategy.getBid(outcomespace,utilityGoal);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return nextBid;
		}
	}
	
}