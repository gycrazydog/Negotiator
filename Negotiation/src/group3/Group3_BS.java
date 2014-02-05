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
 * Adapted mix strategies here, first offer is our bid with best utility.And everytime making bidding,try to tradeoff 
 * by looking for a good enough alternative for last bid of opponent. If it fails, adapt opponent model and 
 * concede goalUtility to find a bid. 
 * 
 * TeamWork: Canran Gou and Shijie Li
 * 
 * @author Canran Gou
 */
public class Group3_BS extends OfferingStrategy {
	private double Pmax;
	SortedOutcomeSpace outcomespace;
	double reserveU = 0.7;// reserve utility in the end while bidding utility is decreasing
	double tradeoffU = 0.85;//reserve utility while making tradeoff
	public Group3_BS(){}
	
	public Group3_BS(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, double e, double k, double max, double min){
		this.Pmax = max;
		this.negotiationSession = negoSession;
		outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
		negotiationSession.setOutcomeSpace(outcomespace);
		this.opponentModel = model;
		this.omStrategy = oms;	
	}
	
	/**
	 * Method which initializes the agent by setting all parameters.
	 * need to find maxUtility for us for concession later
	 */
	public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, HashMap<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
			
		outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
		negotiationSession.setOutcomeSpace(outcomespace);
		
		if (parameters.get("max") != null) {
			Pmax= parameters.get("max");
		} else {
			BidDetails maxBid = negoSession.getMaxBidinDomain();
			Pmax = maxBid.getMyUndiscountedUtil();
		}
			
		this.opponentModel = model;
		this.omStrategy = oms;
	}

	@Override
	public BidDetails determineOpeningBid() {
		//first bid, show our best bid
		return outcomespace.getMaxBidPossible();
	}

	//
	@Override
	public BidDetails determineNextBid() {
		double time = negotiationSession.getTime();
		BidDetails lastOpponentBid = negotiationSession.getOpponentBidHistory().getLastBidDetails();
		double utilityGoal;
			//try to find a tradeoff bid for current opponent bid, which is at least better than tradeoffUtility
			try{
				double bestUtil = tradeoffU;
				BidDetails bestBid = null;
				int issuenum = negotiationSession.getIssues().size();
				//iterate all bids to find tradeoff bids.
				for(BidDetails bd : outcomespace.getAllOutcomes())
				{
					int diff = 0;
					for(int i = 1 ; i <= issuenum ; i ++ )
					{
						//if find different values attached to the same issue of opponent bid
						if(!bd.getBid().getValue(i).equals(lastOpponentBid.getBid().getValue(i)))
							diff++;
					}
					double curUtility = negotiationSession.getUtilitySpace().getUtility(bd.getBid());
					/*update best tradeoff bid,if difference between a bid and opponent bid is more than 2 issues, 
					 *it will not be considered
					 */
					if(diff <= 2&&curUtility>bestUtil)
					{
						bestUtil = curUtility;
						bestBid = bd;
					}
				}
				if(bestBid != null)
					return bestBid;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//if fails to find a good tradeoff, just set a goalUtility,which concede with time and try to find nearest bid to it
			utilityGoal = Pmax-(Pmax-reserveU)*time;
			//adapt opponentModel to find the best bid for opponent
			if (opponentModel instanceof NoModel) {
				nextBid = negotiationSession.getOutcomeSpace().getBidNearUtility(utilityGoal);
			} else {
				nextBid = omStrategy.getBid(outcomespace.getAllOutcomes());
			try {
				double res = negotiationSession.getUtilitySpace().getUtility(nextBid.getBid());
				//if the utility of bid for us is not better than goalUtility, get the nearest bid to it
				if(res<utilityGoal)
					nextBid = omStrategy.getBid(outcomespace,utilityGoal);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//}
			}
			return nextBid;
	}
	
}