package cis501.submission;

import cis501.Direction;
import cis501.IDirectionPredictor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DirPredBimodalTest {
    private IDirectionPredictor bimodal;

    @Before
    public void setUp() throws Exception {
        // Runs before each test...() method

        bimodal = new DirPredBimodal(3/*index bits*/);;
    }

    @Test
    public void testBimodalTakenSaturation() {
        for (int i = 0; i < 10; i++) {
            bimodal.train(0, Direction.Taken);
        }
        bimodal.train(0, Direction.NotTaken);
        bimodal.train(0, Direction.NotTaken);
        assertEquals(Direction.NotTaken, bimodal.predict(0));
    }

    @Test
    public void testBimodalNotTakenSaturation() {
        for (int i = 0; i < 10; i++) {
            bimodal.train(0, Direction.NotTaken);
        }
        bimodal.train(0, Direction.Taken);
        bimodal.train(0, Direction.Taken);
        assertEquals(Direction.Taken, bimodal.predict(0));
    }

    @Test
    public void testWeaklyNotTaken () {
        bimodal.train(0, Direction.Taken);
        assertEquals(Direction.NotTaken, bimodal.predict(0));
    }

    @Test
    public void testWeaklyTaken () {
        bimodal.train(0, Direction.Taken);
        bimodal.train(0, Direction.Taken);
        assertEquals(Direction.Taken, bimodal.predict(0));
    }

    @Test
    public void testCounterIncrement () {
        assertEquals(Direction.NotTaken, bimodal.predict(0));
        bimodal.train(0, Direction.Taken);
        assertEquals(Direction.NotTaken, bimodal.predict(0));
        bimodal.train(0, Direction.Taken);
        assertEquals(Direction.Taken, bimodal.predict(0));
        bimodal.train(0, Direction.Taken);
        assertEquals(Direction.Taken, bimodal.predict(0));
        bimodal.train(0, Direction.Taken);
        assertEquals(Direction.Taken, bimodal.predict(0));

    }

    @Test
    public void testCounterDecrement () {
        bimodal.train(0, Direction.Taken);
        bimodal.train(0, Direction.Taken);
        bimodal.train(0, Direction.Taken);
        bimodal.train(0, Direction.Taken);
        assertEquals(Direction.Taken, bimodal.predict(0));
        bimodal.train(0, Direction.NotTaken);
        assertEquals(Direction.Taken, bimodal.predict(0));
        bimodal.train(0, Direction.NotTaken);
        assertEquals(Direction.NotTaken, bimodal.predict(0));
        bimodal.train(0, Direction.NotTaken);
        assertEquals(Direction.NotTaken, bimodal.predict(0));

    }


    @Test
    public void testIndexingLargeNegativePredict () {
        assertEquals(Direction.NotTaken, bimodal.predict(-9)); //initial
        bimodal.train(7, Direction.Taken); //-9 in twos compliment is 1..10111 truncating to 3 bits it should hash to 7
        bimodal.train(7, Direction.Taken);
        assertEquals(Direction.Taken, bimodal.predict(-9));
    }

    @Test
    public void testIndexingLargeNegativeTrain () {
        assertEquals(Direction.NotTaken, bimodal.predict(-9)); //initial
        bimodal.train(-9, Direction.Taken); //-9 in twos compliment is 1..10111 truncating to 3 bits it should hash to 7
        bimodal.train(-9, Direction.Taken);
        assertEquals(Direction.Taken, bimodal.predict(7));
    }

    @Test
    public void testIndexingLargePositivePredict () {
        assertEquals(Direction.NotTaken, bimodal.predict(9)); //initial
        bimodal.train(1, Direction.Taken); //9 in twos compliment is 0..01001 truncating to 3 bits it should hash to 1
        bimodal.train(1, Direction.Taken);
        assertEquals(Direction.Taken, bimodal.predict(9));
    }

    @Test
    public void testIndexingLargePositiveTrain () {
        assertEquals(Direction.NotTaken, bimodal.predict(9)); //initial
        bimodal.train(9, Direction.Taken); //9 in twos compliment is 0..01001 truncating to 3 bits it should hash to 1
        bimodal.train(9, Direction.Taken);
        assertEquals(Direction.Taken, bimodal.predict(1));
        assertEquals(Direction.Taken, bimodal.predict(9));
    }



}

