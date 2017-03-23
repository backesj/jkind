// Generated from MathSat.g4 by ANTLR 4.4
package jkind.solvers.mathsat;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class MathSatLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.4", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__5=1, T__4=2, T__3=3, T__2=4, T__1=5, T__0=6, BOOL=7, INT=8, ID=9, WS=10, 
		ERROR=11;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] tokenNames = {
		"'\\u0000'", "'\\u0001'", "'\\u0002'", "'\\u0003'", "'\\u0004'", "'\\u0005'", 
		"'\\u0006'", "'\\u0007'", "'\b'", "'\t'", "'\n'", "'\\u000B'"
	};
	public static final String[] ruleNames = {
		"T__5", "T__4", "T__3", "T__2", "T__1", "T__0", "BOOL", "DIGIT", "SYMBOL", 
		"INT", "ID", "WS", "ERROR"
	};


	public MathSatLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "MathSat.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\rT\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5"+
		"\b9\n\b\3\t\3\t\3\n\3\n\3\13\6\13@\n\13\r\13\16\13A\3\f\3\f\3\f\7\fG\n"+
		"\f\f\f\16\fJ\13\f\3\r\6\rM\n\r\r\r\16\rN\3\r\3\r\3\16\3\16\2\2\17\3\3"+
		"\5\4\7\5\t\6\13\7\r\b\17\t\21\2\23\2\25\n\27\13\31\f\33\r\3\2\5\3\2\62"+
		";\b\2##%\'\60\60Bac|\u0080\u0080\5\2\13\f\16\17\"\"V\2\3\3\2\2\2\2\5\3"+
		"\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2"+
		"\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\3\35\3\2\2\2\5\37\3"+
		"\2\2\2\7\'\3\2\2\2\t)\3\2\2\2\13+\3\2\2\2\r-\3\2\2\2\178\3\2\2\2\21:\3"+
		"\2\2\2\23<\3\2\2\2\25?\3\2\2\2\27C\3\2\2\2\31L\3\2\2\2\33R\3\2\2\2\35"+
		"\36\7\61\2\2\36\4\3\2\2\2\37 \7v\2\2 !\7q\2\2!\"\7a\2\2\"#\7t\2\2#$\7"+
		"g\2\2$%\7c\2\2%&\7n\2\2&\6\3\2\2\2\'(\7*\2\2(\b\3\2\2\2)*\7+\2\2*\n\3"+
		"\2\2\2+,\7~\2\2,\f\3\2\2\2-.\7/\2\2.\16\3\2\2\2/\60\7v\2\2\60\61\7t\2"+
		"\2\61\62\7w\2\2\629\7g\2\2\63\64\7h\2\2\64\65\7c\2\2\65\66\7n\2\2\66\67"+
		"\7u\2\2\679\7g\2\28/\3\2\2\28\63\3\2\2\29\20\3\2\2\2:;\t\2\2\2;\22\3\2"+
		"\2\2<=\t\3\2\2=\24\3\2\2\2>@\5\21\t\2?>\3\2\2\2@A\3\2\2\2A?\3\2\2\2AB"+
		"\3\2\2\2B\26\3\2\2\2CH\5\23\n\2DG\5\23\n\2EG\5\21\t\2FD\3\2\2\2FE\3\2"+
		"\2\2GJ\3\2\2\2HF\3\2\2\2HI\3\2\2\2I\30\3\2\2\2JH\3\2\2\2KM\t\4\2\2LK\3"+
		"\2\2\2MN\3\2\2\2NL\3\2\2\2NO\3\2\2\2OP\3\2\2\2PQ\b\r\2\2Q\32\3\2\2\2R"+
		"S\13\2\2\2S\34\3\2\2\2\b\28AFHN\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}