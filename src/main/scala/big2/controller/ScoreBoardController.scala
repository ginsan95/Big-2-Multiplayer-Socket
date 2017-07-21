package big2.controller

import big2.model._;
import big2.Application;
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


@sfxml
class ScoreBoardController(
    private val playerLabel1:Label,
    private val playerLabel2:Label,
    private val playerLabel3:Label,
    private val playerLabel4:Label,
    private val scoreLabel1:Label,
    private val scoreLabel2:Label,
    private val scoreLabel3:Label,
    private val scoreLabel4:Label,
    private val earnedScoreLabel1:Label,
    private val earnedScoreLabel2:Label,
    private val earnedScoreLabel3:Label,
    private val earnedScoreLabel4:Label,
    private val winnerLabel:Label,
    private val bgImgView:ImageView)
  {
    private val resourceIS = getClass.getResourceAsStream("/big2/view/card_background.jpg");
    private val bgImg = new Image(resourceIS);
    bgImgView.image = bgImg;
    
    private val playersLabel:Array[Label] = new Array[Label](Big2Repository.players.length);
    playersLabel(0) = playerLabel1;
    playersLabel(1) = playerLabel2;
    playersLabel(2) = playerLabel3;
    playersLabel(3) = playerLabel4;
    
    private val scoresLabel:Array[Label] = new Array[Label](Big2Repository.players.length);
    scoresLabel(0) = scoreLabel1;
    scoresLabel(1) = scoreLabel2;
    scoresLabel(2) = scoreLabel3;
    scoresLabel(3) = scoreLabel4;
    
    private val earnedScoresLabel:Array[Label] = new Array[Label](Big2Repository.players.length);
    earnedScoresLabel(0) = earnedScoreLabel1;
    earnedScoresLabel(1) = earnedScoreLabel2;
    earnedScoresLabel(2) = earnedScoreLabel3;
    earnedScoresLabel(3) = earnedScoreLabel4;
    
    private var playerIndex:Int= Big2Repository.winnerIndex
    displayData();
    
    def displayData()
    {
      winnerLabel.text = Big2Repository.players(playerIndex).name + " win the game"
      winnerLabel.setStyle("-fx-font-weight: bold")
      calculateScore()
      for(i <- 0 until playersLabel.length)
      {
        val name = Big2Repository.players(i).name;
        val cardAmount = Big2Repository.players(i).cardsAmount;
        val score = Big2Repository.players(i).score;
        val earnedScore = Big2Repository.players(i).earnedScore;
        playersLabel(i).text = s"$name ($cardAmount)";
        scoresLabel(i).text = s"$score ";
        if(i!=playerIndex)
        {
          if(i == Big2Repository.penaltyIndex)
          {
            earnedScoresLabel(i).text = s"($earnedScore) ILLEGAL ASSIST"
          }
          else
          {
            earnedScoresLabel(i).text = s"($earnedScore)"
          }
          earnedScoresLabel(i).textFill = Color.Red
        }
        else
        {
          earnedScoresLabel(i).text = s"(+$earnedScore)"
          earnedScoresLabel(i).textFill = Color.Blue
        }
        //playersLabel(i).textFill = Color.Black; no nid
        //scoresLabel(i).textFill = Color.Black;
        if(i==Big2Repository.myIndex)
        {
          playersLabel(i).setStyle("-fx-font-weight: bold")
          scoresLabel(i).setStyle("-fx-font-weight: bold")
          earnedScoresLabel(i).setStyle("-fx-font-weight: bold")
        }
      }
    }
    
    def calculateScore()
    {
      var multiplier:Double = 1;
      var loser:Buffer[Player] = new ArrayBuffer[Player]()
      for(y<-0 until Big2Repository.players.length)
      {
        if(y!=playerIndex)
        {
           if(y == Big2Repository.penaltyIndex)
           {
             multiplier = 1.5;
           }
           Big2Repository.players(y).calculateLoserScore(multiplier)
           loser+=Big2Repository.players(y)
           multiplier = 1;
          }
       }
      Big2Repository.players(playerIndex).calculateWinnerScore(loser) 
    }
    
    def handleNextRound(action : ActionEvent) = 
    {
        if(Big2Repository.myIndex == 0) //indicate host
        {
          //dont need because the way you calculate score u overwrite the earnedscore
          /*for(x<-0 until Big2Repository.players.length)
          {
            Big2Repository.players(x).earnedScore=0;
          }*/
          Application.hostNewRound();
        } 
     }
   
    }