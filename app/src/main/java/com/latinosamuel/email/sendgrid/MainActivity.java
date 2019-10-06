package com.latinosamuel.email.sendgrid;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final int ADD_ATTACHMENT = 1;

    // Views
    @BindView(R.id.to_editText) EditText mToEditText;
    @BindView(R.id.from_editText) EditText mFromEditText;
    @BindView(R.id.subject_editText) EditText mSubjectEditText;
    @BindView(R.id.message_editText) EditText mMessageEditText;
    @BindView(R.id.attachment_button) Button mAttachmentButton;
    @BindView(R.id.attachment_textView) TextView mAttachmentTextView;

    // Attachment fields
    private Uri selectedImageURI;
    private String attachmentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

    }

    @OnClick(R.id.attachment_button)
    public void attachmentFile(){
        if (selectedImageURI == null) {
            // Start get image intent if no image to attach to email
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, ADD_ATTACHMENT);
        } else {
            // Remove image attachment
            mAttachmentButton.setText("Add Attachment");
            mAttachmentTextView.setVisibility(View.GONE);
            selectedImageURI = null;
        }
    }

    @OnClick(R.id.send_email_button)
    public void sendEmail(){
        // Start send email AsyncTask
        SendEmailASyncTask task = new SendEmailASyncTask(MainActivity.this,
                mToEditText.getText().toString(),
                mFromEditText.getText().toString(),
                mSubjectEditText.getText().toString(),
                mMessageEditText.getText().toString(),
                selectedImageURI,
                attachmentName);
        task.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_ATTACHMENT) {
            if(resultCode==RESULT_OK){
                // Get image from result intent
                selectedImageURI = data.getData();
                ContentResolver contentResolver = getContentResolver();
                Log.d("SendAppExample", "Image Uri: " + selectedImageURI);

                // Get image attachment filename
                attachmentName = "";
                Cursor c = contentResolver.query(selectedImageURI, null, null, null, null);
                if (c != null && c.moveToFirst()) {
                    attachmentName = c.getString(c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                }

                // Update views to show attachment
                mAttachmentTextView.setVisibility(View.VISIBLE);
                mAttachmentTextView.setText(attachmentName);
                mAttachmentButton.setText("Remove Attachment");
            }
        }
    }

    /**
     * AsyncTask that composes and sends email
     */
    private static class SendEmailASyncTask extends AsyncTask<Void, Void, Void> {

        private Context mAppContext;
        private String mMsgResponse;

        private String mTo;
        private String mFrom;
        private String mSubject;
        private String mText;
        private Uri mUri;
        private String mAttachmentName;

        public SendEmailASyncTask(Context context, String mTo, String mFrom, String mSubject,
                                  String mText, Uri mUri, String mAttachmentName) {
            this.mAppContext = context.getApplicationContext();
            this.mTo = mTo;
            this.mFrom = mFrom;
            this.mSubject = mSubject;
            this.mText = mText;
            this.mUri = mUri;
            this.mAttachmentName = mAttachmentName;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                SendGrid sendgrid = new SendGrid("API_KEY");
                SendGrid.Email email = new SendGrid.Email();

                // Get values from edit text to compose email
                // TODO: Validate edit texts
                email.addTo(mTo);
                email.setFrom(mFrom);
                email.setSubject(mSubject);
                email.setText(mText);

                // Attach image
                if (mUri != null) {
                    email.addAttachment(mAttachmentName, mAppContext.getContentResolver().openInputStream(mUri));
                }

                // Send email, execute http request
                SendGrid.Response response = sendgrid.send(email);
                mMsgResponse = response.getMessage();

                Log.d("SendAppExample", mMsgResponse);

            } catch (SendGridException | IOException e) {
                Log.e("SendAppExample", e.toString());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Toast.makeText(mAppContext, mMsgResponse, Toast.LENGTH_SHORT).show();
        }
    }
}