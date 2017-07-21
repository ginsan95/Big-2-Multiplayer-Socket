package big2.controller

import big2.model._;
import big2.Application;
import big2.view.CardView;
import big2.repository.Big2Repository;

import scalafx.scene.control._;
import scalafx.scene.image.{Image,ImageView};
import scalafx.scene.layout._;
import scalafx.event.ActionEvent;
import scalafx.scene.control.Alert.AlertType
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.scene.text.{Font,FontWeight};
import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap};
import scala.util.control.Breaks;
import scalafx.scene.paint.Color;

/**
 * @author AveryChoke
 */
@sfxml
class GameplayController(
    private val myLabel:Label,
    private val player1Label:Label,
    private val player2Label:Label,
    private val player3Label:Label,
    private val myCardsGridPane:GridPane,
    private val myTableGridPane:GridPane,
    private val player1TableGridPane:GridPane,
    private val player2TableGridPane:GridPane,
    private val player3TableGridPane:GridPane,
    private val playCardButton:Button,
    private val passButton:Button,
    private val tableImageView:ImageView)
{
  //reset the previous round data
  Big2Repository.resetRound();
  
  //set the image table
  private val resourceIS = getClass.getResourceAsStream("/big2/view/table.png");
  private val tableImg = new Image(resourceIS);
  tableImageView.image = tableImg;
  
  private val myPlayer:Player = Big2Repository.myPlayer;
  passButton.disable = true; //disable pass button at start
  
  //put labels into array for easy access
  private val playersLabel:Array[Label] = new Array[Label](Big2Repository.players.length);
  playersLabel(Big2Repository.myIndex) = myLabel;
  playersLabel(Big2Repository.nextPlayerIndex(Big2Repository.myIndex,1)) = player1Label;
  playersLabel(Big2Repository.nextPlayerIndex(Big2Repository.myIndex,2)) = player2Label;
  playersLabel(Big2Repository.nextPlayerIndex(Big2Repository.myIndex,3)) = player3Label;
  
  //put table girdpanes into array for easy access
  private val tablesGridPane:Array[GridPane] = new Array[GridPane](Big2Repository.players.length);
  tablesGridPane(Big2Repository.myIndex) = myTableGridPane;
  tablesGridPane(Big2Repository.nextPlayerIndex(Big2Repository.myIndex,1)) = player1TableGridPane;
  tablesGridPane(Big2Repository.nextPlayerIndex(Big2Repository.myIndex,2)) = player2TableGridPane;
  tablesGridPane(Big2Repository.nextPlayerIndex(Big2Repository.myIndex,3)) = player3TableGridPane;
  
  //display the data
  displayData();
  //remove the pass label for first round
  for(table <- tablesGridPane)
  {
    table.children.clear;
  }
  
  //check if I get 4 2s card - straight win
  if(Big2Rules.startWithAllBigTwo(myPlayer.cards))
  {
    println("all big 2");
    winGame();
  }
  
  //display my cards
  displayCards(myPlayer.cards, 75, 100, myCardsGridPane, 7, true);
  
  
  //method to display the data of each turn
  def displayData()
  { 
    //set players labels
    for(i <- 0 until playersLabel.length)
    {
      val name = Big2Repository.players(i).name;
      val cardAmt = Big2Repository.players(i).cardsAmount;
      playersLabel(i).text = s"$name ($cardAmt)";
      playersLabel(i).textFill = Color.Black;
      if(i == Big2Repository.currentTurnIndex)
      {
        playersLabel(i).textFill = Color.Red;
      }
    }
    
    //get the table of the previous player
    val preTableGridPane:GridPane = tablesGridPane(Big2Repository.prePlayerIndex(
        Big2Repository.currentTurnIndex, 1));
    
    if(Big2Repository.passCount>0) //this indicate previous play passed
    {
      //clear the table
      preTableGridPane.children.clear();
      //display pass
      val passLabel:Label = Label("PASS");
      passLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 15));
      preTableGridPane.add(passLabel,2,0);
    }
    else
    {
      //display table cards
      displayCards(Big2Repository.tableCards, 55, 75, preTableGridPane, 5, false);
    }
    
    //clear current player table to further indicate his turn
    tablesGridPane(Big2Repository.currentTurnIndex).children.clear();
    
    //disable enable buttons
    if(Big2Repository.isMyTurn)
    {
      //clear my table
      tablesGridPane(Big2Repository.myIndex).children.clear;
      
      playCardButton.disable = false;
      if(!Big2Repository.isAllPass)
      {
        passButton.disable = false;
      }
    }
    else
    {
      playCardButton.disable = true;
      passButton.disable = true;
      Big2Repository.passCount = 0; //reset the passCount
    }
  }
  
  private def winGame()
  {
    //create a thread to inform the server and update everyone
    new Thread(new PlayerSendIndexThread(Big2Repository.PORT_NUM, Big2Repository.hostAddress,
        0, Big2Repository.myIndex, HostRequest.WIN_GAME)).start();
  }
  
  private def displayCards(cards:Buffer[Card], cardWidth:Double, cardHeight:Double, 
      girdPane:GridPane, colSize:Int, clickable:Boolean)
  {
    //create card view
    val cardViews:Buffer[CardView] = new ArrayBuffer[CardView]();
    for(card <- cards)
    {
      if(clickable)
      {
        cardViews += new CardView(card, myPlayer, cardWidth, cardHeight);
        if(card.compareTo(Deck.d3Card)==0) //auto select diamond 3
        {
          cardViews(cardViews.length-1).select();
        }
      }
      else
      {
        cardViews += new CardView(card, cardWidth, cardHeight);
      }
    }
    
    //display card views
    displayCardViews(cardViews, girdPane, colSize);
  }
  
  private def displayCardViews(cardViews:Buffer[CardView], gridPane:GridPane, colSize:Int)
  {
    var row = 0; 
    var col = 0;
    gridPane.children.clear();
    for(cardView <- cardViews)
    {
      gridPane.add(cardView,col,row);
      col+=1;
      if(col>=colSize)
      {
        row+=1;
        col=0;
      }
    }
  }
  
  //check if will get penalty - for play card
  private def getPenalty(selectedCards:Buffer[Card]):Boolean =
  {
    return (Big2Repository.nextPlayer.cardsAmount==1 &&
        selectedCards.length==1 && selectedCards(0).compareTo(myPlayer.cards.last)!=0);
  }
   //check if will get penalty - for pass
  private def getPenalty:Boolean=
  {
    return (Big2Repository.nextPlayer.cardsAmount==1 &&
        Big2Rules.evaluate(ArrayBuffer(myPlayer.cards.last),Big2Repository.tableCards));
  }
  
  //play card button
  def handlePlayCard(action : ActionEvent) = 
  {
    //get selected cards from player
    val selectedCards:Buffer[Card] = myPlayer.selectedCards.sorted;
    
    //evaluate my cards
    if(((Big2Repository.isAllPass && Big2Rules.evaluateValidity(selectedCards)._1 != null)
          || Big2Rules.evaluate(selectedCards,Big2Repository.tableCards))
        && !Big2Rules.endWithBigTwo(selectedCards, myPlayer.cards))
    { 
      //check penalty
      Big2Repository.penaltyIndex = if (getPenalty(selectedCards)) Big2Repository.myIndex else -1;
      
      //change the table cards
      Big2Repository.tableCards.clear();
      Big2Repository.tableCards ++= selectedCards;
      
      //play the card - remove cards & clear selected & update amt
      myPlayer.playCard();
      
      //reset the hand
      displayCards(myPlayer.cards, 75, 100, myCardsGridPane, 7, true);
      
      //create a thread to inform the server and update everyone
      val t:Thread = new Thread(new PlayerPlayCardThread(Big2Repository.PORT_NUM, Big2Repository.hostAddress,
          0, Big2Repository.tableCards.asInstanceOf[ArrayBuffer[Card]]));
      t.start();
      //wait for thread to finish
      t.join();
      
      //go to next player
      Big2Repository.currentTurnIndex = Big2Repository.nextPlayerIndex(Big2Repository.currentTurnIndex,1);
      
      //reset pass count
      Big2Repository.passCount = 0;
      
      //update my view
      displayData();
      
      //check if win game
      if(myPlayer.cardsAmount<=0)
      {
        winGame();
      }
    }
    else
    {
      new Alert(AlertType.Error){
        initOwner(Application.stage)
        title       = "Invalid Play"
        headerText  = "Not a valid combination"
        contentText = "Please select a valid combination to play."
      }.showAndWait();
    }
  }
  
  //reset button
  def handleReset(action : ActionEvent) = 
  {
    //clear player selected card
    myPlayer.selectedCards.clear();
    //reset the hand
    displayCards(myPlayer.cards, 75, 100, myCardsGridPane, 7, true);
  }
  
  //pass button
  def handlePass(action : ActionEvent) =
  {
    //add pass count
    Big2Repository.passCount += 1;
    
    //check penalty
    Big2Repository.penaltyIndex = if (getPenalty) Big2Repository.myIndex else -1;
    
    //create a thread to inform the server and update everyone
    val t:Thread = new Thread(new PlayerPassThread(Big2Repository.PORT_NUM,
        Big2Repository.hostAddress, 0, Big2Repository.passCount));
    t.start();
    //wait for thread to finish
    t.join();
    
    //go to next player
    Big2Repository.currentTurnIndex = Big2Repository.nextPlayerIndex(Big2Repository.currentTurnIndex,1);
    
    //update my view
    displayData();
  }
}