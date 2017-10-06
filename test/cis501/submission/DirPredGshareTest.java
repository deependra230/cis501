package cis501.submission;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import cis501.BranchPredictor;
import cis501.Direction;
import cis501.IDirectionPredictor;

public class DirPredGshareTest {
    private IDirectionPredictor gshare;
    
    @Before
    public void setUp() throws Exception {
        // Runs before each test...() method
   
        gshare = new DirPredGshare(3/*index bits*/, 3/*history bits*/);
    }
    
    @Test
    public void TestFullShiftTaken() {
    	gshare.train(0, Direction.Taken); //000 ^ 000 = 0
    	//history 001
    	assertEquals(Direction.NotTaken,gshare.predict(1)); //001 ^ 001 = 000
    	gshare.train(1, Direction.Taken); //001 ^ 001 = 000
    	//history 011
    	assertEquals(Direction.Taken,gshare.predict(3)); //011 ^ 011 = 000
    	gshare.train(3, Direction.Taken); // 011 ^ 011 = 0
    	//history 111
    	assertEquals(Direction.Taken,gshare.predict(7)); //111 ^ 111 = 0
    	gshare.train(7, Direction.Taken); /// 111 ^ 111 = 0
    	//history 111
    	assertEquals(Direction.Taken,gshare.predict(7)); //111 ^ 111 = 0
    }
    
    @Test
    public void TestFullShiftNotTaken() {
    	gshare.train(0, Direction.Taken); //000 ^ 000 = 0
    	//history 001
    	assertEquals(Direction.NotTaken,gshare.predict(1)); //001 ^ 001 = 000
    	gshare.train(1, Direction.Taken); //001 ^ 001 = 000
    	//history 011
    	assertEquals(Direction.Taken,gshare.predict(3)); //011 ^ 011 = 000
    	gshare.train(3, Direction.Taken); // 011 ^ 011 = 0
    	//history 111
    	assertEquals(Direction.Taken,gshare.predict(7)); //111 ^ 111 = 0
    	gshare.train(7, Direction.NotTaken); /// 111 ^ 111 = 0
    	//history 110
    	assertEquals(Direction.Taken,gshare.predict(6)); //110 ^ 110 = 0
    	gshare.train(6, Direction.NotTaken); //110 ^ 110 = 0
    	//history 100
    	assertEquals(Direction.NotTaken,gshare.predict(4)); //100 ^ 100 = 0
    	gshare.train(4, Direction.NotTaken); // 100 ^ 100 = 0
    	//history 000;
    	assertEquals(Direction.NotTaken, gshare.predict(0)); //000 ^ 000 = 0
    }
   
    

}
