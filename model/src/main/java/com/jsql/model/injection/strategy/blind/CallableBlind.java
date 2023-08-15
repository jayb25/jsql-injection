package com.jsql.model.injection.strategy.blind;

import com.jsql.model.InjectionModel;
import com.jsql.model.injection.strategy.blind.AbstractInjectionBoolean.BooleanMode;
import com.jsql.model.injection.strategy.blind.patch.Diff;
import com.jsql.model.injection.strategy.blind.patch.DiffMatchPatch;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Define a call HTTP to the server, require the associated url, character
 * position and bit. Opcodes represents the differences between
 * the TRUE page, and the resulting page.
 */
public class CallableBlind extends AbstractCallableBoolean<CallableBlind> {
    
    // List of differences found between the TRUE page, and the present page
    private LinkedList<Diff> opcodes = new LinkedList<>();
    
    private static final DiffMatchPatch DIFFMATCHPATCH = new DiffMatchPatch();

    private final InjectionBlind injectionBlind;
    
    private final InjectionModel injectionModel;
    private final String metadataInjectionProcess;
    
    /**
     * Constructor for preparation and blind confirmation.
     * @param sqlQuery
     * @param injectionBlind
     */
    public CallableBlind(String sqlQuery, InjectionModel injectionModel, InjectionBlind injectionBlind, BooleanMode blindMode, String metadataInjectionProcess) {
        
        this.injectionModel = injectionModel;
        this.injectionBlind = injectionBlind;
        this.metadataInjectionProcess = metadataInjectionProcess;
        this.booleanUrl = this.injectionModel.getMediatorVendor().getVendor().instance().sqlTestBlind(sqlQuery, blindMode);
    }
    
    /**
     * Constructor for bit test.
     * @param sqlQuery
     * @param indexCharacter
     * @param bit
     * @param injectionModel
     */
    public CallableBlind(String sqlQuery, int indexCharacter, int bit, InjectionModel injectionModel, InjectionBlind injectionBlind, BooleanMode blindMode, String metadataInjectionProcess) {
        
        this(sqlQuery, injectionModel, injectionBlind, blindMode, metadataInjectionProcess);
        this.booleanUrl = this.injectionModel.getMediatorVendor().getVendor().instance().sqlBitTestBlind(sqlQuery, indexCharacter, bit, blindMode);
        this.currentIndex = indexCharacter;
        this.currentBit = bit;
    }
    
    /**
     * Constructor for length test.
     * @param sqlQuery
     * @param indexCharacter
     * @param injectionModel
     */
    public CallableBlind(String sqlQuery, int indexCharacter, InjectionModel injectionModel, InjectionBlind injectionBlind, BooleanMode blindMode, String metadataInjectionProcess) {
        
        this(sqlQuery, injectionModel, injectionBlind, blindMode, metadataInjectionProcess);
        this.booleanUrl = this.injectionModel.getMediatorVendor().getVendor().instance().sqlLengthTestBlind(sqlQuery, indexCharacter, blindMode);
        this.isTestingLength = true;
    }

    /**
     * Check if a result page means the SQL query is true,
     * confirm that nothing in the resulting page is also defined
     * in the pages from every FALSE SQL queries.
     * @return true if the current SQL query is true
     */
    @Override
    public boolean isTrue() {

        // Fix #95426: ConcurrentModificationException on iterator.next()
        List<Diff> copyFalseMarks = new CopyOnWriteArrayList<>(this.injectionBlind.getConstantFalseMark());
        for (Diff falseDiff: copyFalseMarks) {

            // Fix #4386: NullPointerException on contains()
            // opcodes is initialized to an empty new LinkedList<>()
            if (this.opcodes.contains(falseDiff)) {

                return false;
            }
        }
        
        return true;
    }

    /**
     * Process the URL HTTP call, use function inject() from the model.
     * Build the list of differences found between TRUE and the current page.
     * @return Functional Blind Callable
     */
    @Override
    public CallableBlind call() {
        
        String ctnt = this.injectionBlind.callUrl(this.booleanUrl, this.metadataInjectionProcess, this);
        
        this.opcodes = DIFFMATCHPATCH.diffMain(this.injectionBlind.getBlankTrueMark(), ctnt, true);
        
        DIFFMATCHPATCH.diffCleanupEfficiency(this.opcodes);
        
        return this;
    }
    
    public List<Diff> getOpcodes() {
        return this.opcodes;
    }
}
