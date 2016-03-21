package com.giovanniterlingen.windesheim;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class ChooseTypeFragment extends Fragment {

    private final ArrayList<Integer> componentId = new ArrayList<>();
    private final ArrayList<String> componentList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView listView;
    private int type;
    private Context context;
    private ProgressBar spinner;
    private boolean isShowing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        int position = getArguments().getInt("position");
        if (position == 0) {
            type = 1;
        }
        if (position == 1) {
            type = 2;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter == null) {
            new ComponentFetcher().execute();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choose_type, container, false);
        TextView chooseTextview = (TextView) view.findViewById(R.id.choose_textview);
        TextView descriptionTextview = (TextView) view.findViewById(R.id.description_textview);
        EditText dataSearch = (EditText) view.findViewById(R.id.filter_edittext);
        if (type == 1) {
            chooseTextview.setText("Kies je klas");
            descriptionTextview.setText("Je kunt hier je klas zoeken en daar vervolgens op klikken om deze als je standaard rooster in te stellen.");
            dataSearch.setHint("Typ hier je klas");
        }
        if (type == 2) {
            chooseTextview.setText("Kies uw naam of docentcode");
            descriptionTextview.setText("U kunt hier uw docentcode zoeken en daar vervolgens op klikken om deze als uw standaard rooster in te stellen.");
            dataSearch.setHint("Typ hier uw naam of code");
        }
        listView = (ListView) view.findViewById(R.id.listview);
        spinner = (ProgressBar) view.findViewById(R.id.progress_bar);
        dataSearch.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                if (adapter != null) {
                    adapter.getFilter().filter(arg0);
                }
            }

            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                if (adapter == null) {
                    new ComponentFetcher().execute();
                }
            }

            public void afterTextChanged(Editable arg0) {
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("componentId", componentId.get(componentList.indexOf(listView.getItemAtPosition(arg2))).toString());
                editor.putInt("notifications_type", 5);
                editor.putInt("type", type);
                editor.commit();

                ApplicationLoader.restartNotificationThread();

                Intent intent = new Intent(context, ScheduleActivity.class);
                startActivity(intent);
                getActivity().finish();
            }

        });

        return view;
    }


    private void buildClassArray(JSONArray jsonArray) {
        JSONObject jsonObject;
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                jsonObject = jsonArray.getJSONObject(i);
                this.componentList.add(jsonObject.getString("name") + " - " + jsonObject.getString("longName"));
                this.componentId.add(jsonObject.getInt("id"));
            } catch (JSONException e) {
                alertConnectionProblem();
                break;
            }
        }
    }

    private void alertConnectionProblem() {
        if (isShowing || !getUserVisibleHint()) {
            return;
        }
        isShowing = true;
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle("Probleem met verbinden!")
                        .setMessage("De gegevens konden niet worden opgevraagd. Controleer je internetverbinding en probeer het opnieuw.")
                        .setIcon(R.drawable.ic_launcher)
                        .setPositiveButton("Verbinden",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        new ComponentFetcher().execute();
                                        dialog.cancel();
                                        isShowing = false;
                                    }
                                })
                        .setNegativeButton("Annuleren", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                isShowing = false;
                            }
                        }).show();
            }
        });
    }

    private class ComponentFetcher extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            spinner.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                buildClassArray(new JSONObject(ScheduleHandler.getListFromServer(type)).getJSONArray("elements"));
                adapter = new ArrayAdapter<>(context, R.layout.component_adapter_item, R.id.component_item, componentList);
            } catch (Exception e) {
                alertConnectionProblem();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);
            spinner.setVisibility(View.GONE);
            listView.setAdapter(adapter);
        }
    }
}
